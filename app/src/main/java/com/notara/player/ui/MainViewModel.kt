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
import com.notara.player.data.LibraryFilterConfig
import com.notara.player.data.LibraryFilterPreferences
import com.notara.player.data.TrackSortMode
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

data class AbLoopState(
    val startMs: Long? = null,
    val endMs: Long? = null
) {
    val isActive: Boolean get() = startMs != null && endMs != null && endMs > startMs
}

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

    private val _filterConfig = MutableStateFlow(LibraryFilterPreferences.load(application))
    val filterConfig: StateFlow<LibraryFilterConfig> = _filterConfig.asStateFlow()

    private val _rawTracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val rawTracks: StateFlow<List<LocalTrack>> = _rawTracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedFolder = MutableStateFlow("ALL_FOLDER")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val favoriteHashes: StateFlow<Set<String>> = repository.observeFavoriteHashes()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val customCovers: StateFlow<Map<String, String>> = repository.observeAllCustomCovers()
        .map { list -> list.associate { it.trackHash to it.coverPath } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistTrackMap: StateFlow<Map<Long, List<String>>> = repository.observeAllPlaylistRefs()
        .map { refs -> refs.groupBy({ it.playlistId }, { it.trackHash }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Semua folder yang terdeteksi (untuk layar Hide Folder di Settings)
    val allAvailableFolders: StateFlow<List<String>> = _rawTracks
        .map { list -> list.map { it.folderName }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lagu yang terlihat setelah menerapkan Hide Folders & Min Duration
    val visibleTracks: StateFlow<List<LocalTrack>> = combine(_rawTracks, _filterConfig) { raw, cfg ->
        val minMs = cfg.minDurationSec * 1000L
        raw.filter { t ->
            t.durationMs >= minMs && t.folderName !in cfg.hiddenFolders
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleFolders: StateFlow<List<String>> = visibleTracks
        .map { list -> listOf("ALL_FOLDER") + list.map { it.folderName }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("ALL_FOLDER"))

    // Recently Played tracks untuk Home screen
    val recentlyPlayedTracks: StateFlow<List<LocalTrack>> = combine(
        repository.observeRecentlyPlayed(),
        _rawTracks
    ) { history, all ->
        val byHash = all.associateBy { it.trackHash }
        history.mapNotNull { byHash[it.trackHash] }.distinctBy { it.trackHash }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTracks: StateFlow<List<LocalTrack>> = combine(
        visibleTracks,
        _selectedFolder,
        _searchQuery,
        _favoritesOnly,
        favoriteHashes,
        _filterConfig
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val all = values[0] as List<LocalTrack>
        val folder = values[1] as String
        val query = (values[2] as String).trim().lowercase()
        val favOnly = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val favs = values[4] as Set<String>
        val cfg = values[5] as LibraryFilterConfig

        val filtered = all.filter { track ->
            val matchFolder = folder == "ALL_FOLDER" || track.folderName == folder
            val matchFav = !favOnly || track.trackHash in favs
            val matchQuery = query.isEmpty() ||
                track.title.lowercase().contains(query) ||
                track.artist.lowercase().contains(query) ||
                track.album.lowercase().contains(query)
            matchFolder && matchFav && matchQuery
        }

        when (cfg.sortMode) {
            TrackSortMode.DATE_ADDED -> filtered
            TrackSortMode.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            TrackSortMode.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            TrackSortMode.FAVORITES_FIRST -> filtered.sortedByDescending { it.trackHash in favs }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentQueue = MutableStateFlow<List<LocalTrack>>(emptyList())
    val currentQueue: StateFlow<List<LocalTrack>> = _currentQueue.asStateFlow()

    private val _currentTrack = MutableStateFlow<LocalTrack?>(null)
    val currentTrack: StateFlow<LocalTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatOneEnabled = MutableStateFlow(false)
    val repeatOneEnabled: StateFlow<Boolean> = _repeatOneEnabled.asStateFlow()

    private val _abLoop = MutableStateFlow(AbLoopState())
    val abLoop: StateFlow<AbLoopState> = _abLoop.asStateFlow()

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
            if (track == null) flowOf(emptyList()) else repository.observeMarkers(track.trackHash)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transcriptLines: StateFlow<List<TrackTranscriptEntity>> = _currentTrack
        .flatMapLatest { track ->
            if (track == null) flowOf(emptyList()) else repository.observeTranscript(track.trackHash)
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

    private fun updateFilterConfig(transform: (LibraryFilterConfig) -> LibraryFilterConfig) {
        val updated = transform(_filterConfig.value)
        _filterConfig.value = updated
        LibraryFilterPreferences.save(app, updated)
    }

    fun toggleHiddenFolder(folderName: String) = updateFilterConfig { cfg ->
        val next = cfg.hiddenFolders.toMutableSet()
        if (!next.add(folderName)) next.remove(folderName)
        cfg.copy(hiddenFolders = next)
    }

    fun setMinDurationSec(sec: Int) = updateFilterConfig { it.copy(minDurationSec = sec.coerceIn(0, 120)) }

    fun setSortMode(mode: TrackSortMode) = updateFilterConfig { it.copy(sortMode = mode) }

    fun toggleFavoritesOnly() {
        _favoritesOnly.value = !_favoritesOnly.value
    }

    fun toggleFavoriteTrack(trackHash: String) {
        val isFav = trackHash in favoriteHashes.value
        viewModelScope.launch { repository.toggleFavorite(trackHash, isFav) }
    }

    private fun connectMediaController() {
        viewModelScope.launch {
            runCatching {
                val token = SessionToken(app, ComponentName(app, NotaraPlaybackService::class.java))
                val mc = MediaController.Builder(app, token).buildAsync().await()
                mc.repeatMode = if (_repeatOneEnabled.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
                mc.shuffleModeEnabled = _shuffleEnabled.value
                controller = mc
                mc.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val mediaId = mediaItem?.mediaId ?: return
                        val matched = _rawTracks.value.firstOrNull { it.trackHash == mediaId }
                        if (matched != null && matched.trackHash != _currentTrack.value?.trackHash) {
                            _currentTrack.value = matched
                            _durationMs.value = matched.durationMs
                            _selectedMarker.value = null
                            _abLoop.value = AbLoopState()
                            loadWaveformForTrack(matched)
                            viewModelScope.launch { repository.recordPlay(matched.trackHash) }
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

                    // A-B Loop enforcement
                    val loop = _abLoop.value
                    if (loop.isActive && pos >= loop.endMs!!) {
                        mc.seekTo(loop.startMs!!)
                        _currentPositionMs.value = loop.startMs
                    }
                }
                delay(180L)
            }
        }
    }

    fun loadLibraryIfNeeded() {
        if (hasInitializedLibrary) return
        hasInitializedLibrary = true
        viewModelScope.launch(Dispatchers.IO) {
            val cached = repository.loadCachedTracks()
            if (cached.isNotEmpty()) {
                _rawTracks.value = cached
            } else {
                _isScanning.value = true
                _rawTracks.value = repository.scanAndCacheLocalTracks()
                _isScanning.value = false
            }
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _rawTracks.value = repository.scanAndCacheLocalTracks()
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
        val activeQueue = if (queue.any { it.trackHash == track.trackHash }) queue else listOf(track)
        val startIndex = activeQueue.indexOfFirst { it.trackHash == track.trackHash }.coerceAtLeast(0)

        _currentQueue.value = activeQueue
        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L
        _selectedMarker.value = null
        _abLoop.value = AbLoopState()
        loadWaveformForTrack(track)
        viewModelScope.launch { repository.recordPlay(track.trackHash) }

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
        mc.repeatMode = if (_repeatOneEnabled.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
        mc.shuffleModeEnabled = _shuffleEnabled.value
        mc.setMediaItems(mediaItems, startIndex, 0L)
        mc.prepare()
        mc.play()
        NotaraPlaybackService.updateAudioEffects(_effectsState.value)
    }

    fun jumpToQueueIndex(index: Int) {
        val mc = controller ?: return
        val q = _currentQueue.value
        if (index !in q.indices) return
        mc.seekTo(index, 0L)
        mc.play()
    }

    fun toggleShuffle() {
        val next = !_shuffleEnabled.value
        _shuffleEnabled.value = next
        controller?.shuffleModeEnabled = next
    }

    fun toggleRepeatOne() {
        val next = !_repeatOneEnabled.value
        _repeatOneEnabled.value = next
        controller?.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
    }

    /**
     * A-B Loop 3-tahap: (1) Set A -> (2) Set B & Aktifkan Loop -> (3) Matikan Loop.
     */
    fun cycleAbLoop() {
        val now = _currentPositionMs.value
        val cur = _abLoop.value
        _abLoop.value = when {
            cur.startMs == null -> AbLoopState(startMs = now, endMs = null)
            cur.endMs == null -> {
                if (now > cur.startMs + 500L) {
                    AbLoopState(startMs = cur.startMs, endMs = now)
                } else {
                    AbLoopState(startMs = now, endMs = null)
                }
            }
            else -> AbLoopState()
        }
    }

    fun clearAbLoop() {
        _abLoop.value = AbLoopState()
    }

    private fun loadWaveformForTrack(track: LocalTrack) {
        viewModelScope.launch {
            val cached = repository.getCachedWaveform(track.trackHash)
            if (cached != null) {
                _waveformBars.value = cached
            } else {
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

    fun skipNext() {
        val mc = controller ?: return
        val count = mc.mediaItemCount
        if (count <= 0) return
        if (mc.shuffleModeEnabled) {
            mc.seekToNextMediaItem()
        } else {
            val nextIndex = (mc.currentMediaItemIndex + 1) % count
            mc.seekTo(nextIndex, 0L)
        }
        mc.play()
    }

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

    fun updateMarker(marker: TrackMarkerEntity, newTitle: String, newDescription: String, newTimestampMs: Long, newColorHex: String) {
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

    // --- Playlists ---
    fun createPlaylist(name: String, coverUri: Uri?) {
        viewModelScope.launch {
            val savedPath = coverUri?.let { coverStorage.saveCustomCover(it, "pl") }
            repository.createPlaylist(name, savedPath)
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity, newName: String, newCoverUri: Uri?) {
        viewModelScope.launch {
            val savedPath = if (newCoverUri != null) coverStorage.saveCustomCover(newCoverUri, "pl_${playlist.id}")
            else playlist.customCoverPath
            repository.updatePlaylist(playlist.copy(name = newName.trim().ifBlank { playlist.name }, customCoverPath = savedPath))
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
                _duplicatePlaylistAlert.value = DuplicatePlaylistAlert(track.title, playlist.name)
            }
        }
    }

    fun dismissDuplicateAlert() {
        _duplicatePlaylistAlert.value = null
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackHash: String) {
        viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, trackHash) }
    }

    // --- Custom Covers ---
    fun updateTrackCustomCover(trackHash: String, sourceUri: Uri) {
        viewModelScope.launch {
            val savedPath = coverStorage.saveCustomCover(sourceUri, "trk_$trackHash")
            if (savedPath != null) repository.setTrackCustomCover(trackHash, savedPath)
        }
    }

    fun resetTrackCustomCover(trackHash: String) {
        viewModelScope.launch { repository.clearTrackCustomCover(trackHash) }
    }

    // --- Transcripts & Full Backup ---
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
        return BackupExporter.exportMarkersToJson(track.title, track.trackHash, repository.getMarkersOnce(track.trackHash))
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
        return TranscriptParser.exportToLrc(repository.getTranscriptOnce(track.trackHash))
    }

    suspend fun buildFullBackupJson(): String {
        return repository.buildFullBackupJson(_filterConfig.value)
    }

    fun restoreFullBackupJson(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = BackupExporter.parseFullBackup(json)
            if (parsed != null) {
                repository.restoreFullBackup(parsed)
                updateFilterConfig {
                    it.copy(
                        hiddenFolders = parsed.hiddenFolders.toSet(),
                        minDurationSec = parsed.minDurationSec
                    )
                }
                _statusMessage.value = "Backup restored!"
            } else {
                _statusMessage.value = "Invalid backup file"
            }
        }
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
    fun selectEqPreset(preset: EqPreset) = updateEffects { it.copy(eqEnabled = true, eqPreset = preset, eqBandLevelsDb = preset.bandsDb) }

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
