package com.notara.player.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.notara.player.NotaraApp
import com.notara.player.data.BackupExporter
import com.notara.player.data.TranscriptParser
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.db.TrackMarkerEntity
import com.notara.player.data.db.TrackTranscriptEntity
import com.notara.player.data.model.AudioEffectsState
import com.notara.player.data.model.EqPreset
import com.notara.player.data.model.LocalTrack
import com.notara.player.data.model.ReverbOption
import com.notara.player.playback.NotaraPlaybackService
import com.notara.player.ui.i18n.AppLanguage
import com.notara.player.ui.i18n.LanguagePreferences
import com.notara.player.ui.theme.AppThemePreset
import com.notara.player.ui.theme.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

data class DuplicatePlaylistAlert(
    val trackTitle: String,
    val playlistName: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotaraApp
    private val repository = app.repository
    private val waveformExtractor = app.waveformExtractor
    private val coverStorage = app.coverStorage

    private var controller: MediaController? = null
    private var hasInitializedLibrary = false

    private val _themePreset = MutableStateFlow(ThemePreferences.loadTheme(application))
    val themePreset: StateFlow<AppThemePreset> = _themePreset.asStateFlow()

    private val _language = MutableStateFlow(LanguagePreferences.load(application))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _tracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val tracks: StateFlow<List<LocalTrack>> = _tracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedFolder = MutableStateFlow("ALL_FOLDER")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val customCovers: StateFlow<Map<String, String>> = repository.observeAllCustomCovers()
        .map { list -> list.associate { it.trackHash to it.coverPath } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistTrackMap: StateFlow<Map<Long, List<String>>> = repository.observeAllPlaylistRefs()
        .map { refs -> refs.groupBy({ it.playlistId }, { it.trackHash }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val folders: StateFlow<List<String>> = _tracks
        .map { list -> listOf("ALL_FOLDER") + list.map { it.folderName }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("ALL_FOLDER"))

    val filteredTracks: StateFlow<List<LocalTrack>> = combine(
        _tracks,
        _selectedFolder,
        _searchQuery
    ) { all, folder, query ->
        val q = query.trim().lowercase()
        all.filter { track ->
            val matchFolder = folder == "ALL_FOLDER" || track.folderName == folder
            val matchQuery = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q)
            matchFolder && matchQuery
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTrack = MutableStateFlow<LocalTrack?>(null)
    val currentTrack: StateFlow<LocalTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _waveformBars = MutableStateFlow<List<Float>>(emptyList())
    val waveformBars: StateFlow<List<Float>> = _waveformBars.asStateFlow()

    private val _selectedMarker = MutableStateFlow<TrackMarkerEntity?>(null)
    val selectedMarker: StateFlow<TrackMarkerEntity?> = _selectedMarker.asStateFlow()

    private val _effectsState = MutableStateFlow(AudioEffectsState())
    val effectsState: StateFlow<AudioEffectsState> = _effectsState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _duplicatePlaylistAlert = MutableStateFlow<DuplicatePlaylistAlert?>(null)
    val duplicatePlaylistAlert: StateFlow<DuplicatePlaylistAlert?> = _duplicatePlaylistAlert.asStateFlow()

    val markers: StateFlow<List<TrackMarkerEntity>> = _currentTrack
        .flatMapLatest { track ->
            if (track == null) flowOf(emptyList())
            else repository.observeMarkers(track.trackHash)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transcriptLines: StateFlow<List<TrackTranscriptEntity>> = _currentTrack
        .flatMapLatest { track ->
            if (track == null) flowOf(emptyList())
            else repository.observeTranscript(track.trackHash)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        connectMediaController()
        startPositionTicker()
    }

    fun selectTheme(preset: AppThemePreset) {
        _themePreset.value = preset
        ThemePreferences.saveTheme(app, preset)
    }

    fun selectLanguage(lang: AppLanguage) {
        _language.value = lang
        LanguagePreferences.save(app, lang)
    }

    private fun connectMediaController() {
        viewModelScope.launch {
            runCatching {
                val token = SessionToken(
                    app,
                    ComponentName(app, NotaraPlaybackService::class.java)
                )
                val mc = MediaController.Builder(app, token).buildAsync().await()
                mc.repeatMode = Player.REPEAT_MODE_ALL
                controller = mc
                mc.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val mediaId = mediaItem?.mediaId ?: return
                        val matched = _tracks.value.firstOrNull { it.trackHash == mediaId }
                        if (matched != null && matched.trackHash != _currentTrack.value?.trackHash) {
                            _currentTrack.value = matched
                            _durationMs.value = matched.durationMs
                            _selectedMarker.value = null
                            loadWaveformForTrack(matched)
                        }
                    }
                })
                NotaraPlaybackService.updateAudioEffects(_effectsState.value)
            }
        }
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            while (true) {
                val mc = controller
                if (mc != null && mc.playbackState != Player.STATE_IDLE && mc.isPlaying) {
                    val pos = mc.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    val dur = mc.duration
                    if (dur > 0) _durationMs.value = dur
                }
                delay(200L)
            }
        }
    }

    /**
     * Memuat cache Room secara instan (<20ms) saat aplikasi dibuka agar tidak lag.
     * Pemindaian MediaStore hanya berjalan otomatis jika cache masih kosong,
     * atau ketika pengguna menekan tombol Rescan secara manual.
     */
    fun loadLibraryIfNeeded() {
        if (hasInitializedLibrary) return
        hasInitializedLibrary = true
        viewModelScope.launch(Dispatchers.IO) {
            val cached = repository.loadCachedTracks()
            if (cached.isNotEmpty()) {
                _tracks.value = cached
            } else {
                _isScanning.value = true
                val fresh = repository.scanAndCacheLocalTracks()
                _tracks.value = fresh
                _isScanning.value = false
            }
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            val fresh = repository.scanAndCacheLocalTracks()
            _tracks.value = fresh
            _isScanning.value = false
        }
    }

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playTrack(track: LocalTrack, queue: List<LocalTrack> = filteredTracks.value) {
        val activeQueue = if (queue.contains(track)) queue else listOf(track)
        val startIndex = activeQueue.indexOf(track).coerceAtLeast(0)

        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L
        _selectedMarker.value = null
        loadWaveformForTrack(track)

        val mc = controller ?: return
        val mediaItems = activeQueue.map { item ->
            MediaItem.Builder()
                .setMediaId(item.trackHash)
                .setUri(item.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .setArtworkUri(item.albumArtUri)
                        .build()
                )
                .build()
        }
        mc.repeatMode = Player.REPEAT_MODE_ALL
        mc.setMediaItems(mediaItems, startIndex, 0L)
        mc.prepare()
        mc.play()
        NotaraPlaybackService.updateAudioEffects(_effectsState.value)
    }

    private fun loadWaveformForTrack(track: LocalTrack) {
        viewModelScope.launch {
            val cached = repository.getCachedWaveform(track.trackHash)
            if (cached != null) {
                _waveformBars.value = cached
            } else {
                // Tampilkan placeholder sementara lalu decode PCM real RMS di background
                _waveformBars.value = List(68) { 0.18f }
                val extracted = waveformExtractor.extractBars(track.contentUri, track.trackHash)
                if (_currentTrack.value?.trackHash == track.trackHash) {
                    _waveformBars.value = extracted
                }
                repository.saveCachedWaveform(track.trackHash, extracted)
            }
        }
    }

    fun togglePlayPause() {
        val mc = controller ?: return
        if (mc.isPlaying) mc.pause() else mc.play()
    }

    fun seekTo(positionMs: Long) {
        val dur = _durationMs.value.coerceAtLeast(1L)
        val target = positionMs.coerceIn(0L, dur)
        controller?.seekTo(target)
        _currentPositionMs.value = target
    }

    /**
     * Wrap-around Next: jika sudah di lagu terakhir dalam antrean (Semua Lagu/Folder/Playlist),
     * otomatis kembali memutar lagu pertama (index 0).
     */
    fun skipNext() {
        val mc = controller ?: return
        val count = mc.mediaItemCount
        if (count <= 0) return
        val nextIndex = (mc.currentMediaItemIndex + 1) % count
        mc.seekTo(nextIndex, 0L)
        mc.play()
    }

    /**
     * Wrap-around Previous: jika posisi > 3 detik kembali ke awal lagu,
     * jika di lagu pertama mundur ke lagu terakhir.
     */
    fun skipPrevious() {
        val mc = controller ?: return
        val count = mc.mediaItemCount
        if (count <= 0) return
        if (mc.currentPosition > 3000L) {
            mc.seekTo(0L)
        } else {
            val prevIndex = if (mc.currentMediaItemIndex <= 0) count - 1 else mc.currentMediaItemIndex - 1
            mc.seekTo(prevIndex, 0L)
            mc.play()
        }
    }

    // --- Markers ---
    fun jumpToMarker(marker: TrackMarkerEntity) {
        _selectedMarker.value = marker
        seekTo(marker.timestampMs)
        val mc = controller
        if (mc != null && !mc.isPlaying) mc.play()
    }

    fun clearSelectedMarker() {
        _selectedMarker.value = null
    }

    fun addMarker(timestampMs: Long, title: String, description: String, colorHex: String) {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            repository.addMarker(track.trackHash, timestampMs, title, description, colorHex)
            _statusMessage.value = "Pin @ ${formatTimeMs(timestampMs)}"
        }
    }

    fun updateMarker(
        marker: TrackMarkerEntity,
        newTitle: String,
        newDescription: String,
        newTimestampMs: Long,
        newColorHex: String
    ) {
        viewModelScope.launch {
            val updated = marker.copy(
                title = newTitle.trim().ifBlank { "Pin" },
                description = newDescription.trim(),
                timestampMs = newTimestampMs,
                colorHex = newColorHex
            )
            repository.updateMarker(updated)
            if (_selectedMarker.value?.id == marker.id) _selectedMarker.value = updated
        }
    }

    fun deleteMarker(marker: TrackMarkerEntity) {
        viewModelScope.launch {
            repository.deleteMarker(marker)
            if (_selectedMarker.value?.id == marker.id) _selectedMarker.value = null
        }
    }

    // --- Playlists (with strict duplicate guard + popup warning) ---
    fun createPlaylist(name: String, coverUri: Uri?) {
        viewModelScope.launch {
            val savedPath = coverUri?.let { coverStorage.saveCustomCover(it, "pl") }
            repository.createPlaylist(name, savedPath)
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity, newName: String, newCoverUri: Uri?) {
        viewModelScope.launch {
            val savedPath = if (newCoverUri != null) {
                coverStorage.saveCustomCover(newCoverUri, "pl_${playlist.id}")
            } else {
                playlist.customCoverPath
            }
            repository.updatePlaylist(
                playlist.copy(
                    name = newName.trim().ifBlank { playlist.name },
                    customCoverPath = savedPath
                )
            )
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { repository.deletePlaylist(playlist.id) }
    }

    fun addTrackToPlaylist(playlist: PlaylistEntity, track: LocalTrack) {
        viewModelScope.launch {
            val added = repository.addTrackToPlaylistIfAbsent(playlist.id, track.trackHash)
            if (added) {
                _statusMessage.value = "+ ${playlist.name}"
            } else {
                _duplicatePlaylistAlert.value = DuplicatePlaylistAlert(
                    trackTitle = track.title,
                    playlistName = playlist.name
                )
            }
        }
    }

    fun dismissDuplicateAlert() {
        _duplicatePlaylistAlert.value = null
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackHash: String) {
        viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, trackHash) }
    }

    // --- Custom Track Covers ---
    fun updateTrackCustomCover(trackHash: String, sourceUri: Uri) {
        viewModelScope.launch {
            val savedPath = coverStorage.saveCustomCover(sourceUri, "trk_$trackHash")
            if (savedPath != null) repository.setTrackCustomCover(trackHash, savedPath)
        }
    }

    fun resetTrackCustomCover(trackHash: String) {
        viewModelScope.launch { repository.clearTrackCustomCover(trackHash) }
    }

    // --- Transcripts & Backup ---
    fun saveTranscriptText(rawText: String) {
        val track = _currentTrack.value ?: return
        viewModelScope.launch { repository.saveTranscriptFromRaw(track.trackHash, rawText) }
    }

    fun syncTranscriptLineToCurrentTime(line: TrackTranscriptEntity) {
        val nowMs = _currentPositionMs.value
        viewModelScope.launch { repository.syncTranscriptLine(line.id, nowMs) }
    }

    suspend fun buildMarkersExportJson(): String? {
        val track = _currentTrack.value ?: return null
        val list = repository.getMarkersOnce(track.trackHash)
        return BackupExporter.exportMarkersToJson(track.title, track.trackHash, list)
    }

    fun importMarkersJson(json: String) {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            runCatching {
                val parsed = BackupExporter.importMarkersFromJson(json, track.trackHash)
                repository.importMarkers(parsed)
                _statusMessage.value = "+${parsed.size} pins"
            }
        }
    }

    suspend fun buildTranscriptExportLrc(): String? {
        val track = _currentTrack.value ?: return null
        val lines = repository.getTranscriptOnce(track.trackHash)
        return TranscriptParser.exportToLrc(lines)
    }

    // --- Audio Effects ---
    private fun updateEffects(transform: (AudioEffectsState) -> AudioEffectsState) {
        val updated = transform(_effectsState.value)
        _effectsState.value = updated
        NotaraPlaybackService.updateAudioEffects(updated)
    }

    fun setVolumePercent(percent: Int) = updateEffects { it.copy(volumePercent = percent.coerceIn(0, 200)) }
    fun setLimiterEnabled(enabled: Boolean) = updateEffects { it.copy(limiterEnabled = enabled) }
    fun setSpeed(speed: Float) = updateEffects { it.copy(speed = speed.coerceIn(0.5f, 2.0f)) }
    fun setPitch(pitch: Float) = updateEffects { it.copy(pitch = pitch.coerceIn(0.5f, 2.0f)) }
    fun setSkipSilence(enabled: Boolean) = updateEffects { it.copy(skipSilence = enabled) }
    fun setEqEnabled(enabled: Boolean) = updateEffects { it.copy(eqEnabled = enabled) }

    fun selectEqPreset(preset: EqPreset) = updateEffects {
        it.copy(eqEnabled = true, eqPreset = preset, eqBandLevelsDb = preset.bandsDb)
    }

    fun setEqBand(index: Int, db: Int) = updateEffects { state ->
        val mutable = state.eqBandLevelsDb.toMutableList()
        if (index in mutable.indices) mutable[index] = db.coerceIn(-10, 10)
        state.copy(eqEnabled = true, eqPreset = EqPreset.CUSTOM, eqBandLevelsDb = mutable)
    }

    fun setBassBoost(strength: Int) = updateEffects { it.copy(bassBoostStrength = strength.coerceIn(0, 1000)) }
    fun setReverb(reverb: ReverbOption) = updateEffects { it.copy(reverb = reverb) }
    fun resetAudioEffects() = updateEffects { AudioEffectsState() }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
