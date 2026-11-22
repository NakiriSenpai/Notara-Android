package com.notara.player.ui

import android.app.Application
import android.content.ComponentName
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
import com.notara.player.data.db.TrackMarkerEntity
import com.notara.player.data.db.TrackTranscriptEntity
import com.notara.player.data.model.AudioEffectsState
import com.notara.player.data.model.EqPreset
import com.notara.player.data.model.LocalTrack
import com.notara.player.data.model.ReverbOption
import com.notara.player.playback.NotaraPlaybackService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotaraApp
    private val repository = app.repository
    private val waveformExtractor = app.waveformExtractor

    private var controller: MediaController? = null

    private val _tracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val tracks: StateFlow<List<LocalTrack>> = _tracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedFolder = MutableStateFlow("Semua")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val folders: StateFlow<List<String>> = _tracks
        .combine(flowOf(Unit)) { list, _ ->
            listOf("Semua") + list.map { it.folderName }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Semua"))

    val filteredTracks: StateFlow<List<LocalTrack>> = combine(
        _tracks,
        _selectedFolder,
        _searchQuery
    ) { all, folder, query ->
        all.filter { track ->
            val matchFolder = folder == "Semua" || track.folderName == folder
            val q = query.trim().lowercase()
            val matchQuery = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q)
            matchFolder && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private fun connectMediaController() {
        viewModelScope.launch {
            runCatching {
                val token = SessionToken(
                    app,
                    ComponentName(app, NotaraPlaybackService::class.java)
                )
                val mc = MediaController.Builder(app, token).buildAsync().await()
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
                if (mc != null && mc.playbackState != Player.STATE_IDLE) {
                    val pos = mc.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    val dur = mc.duration
                    if (dur > 0) _durationMs.value = dur
                }
                delay(150L)
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = repository.loadLocalTracks()
            _tracks.value = result
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
                val extracted = waveformExtractor.extractBars(track.contentUri, track.trackHash)
                _waveformBars.value = extracted
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
        controller?.seekToNext()
    }

    fun skipPrevious() {
        val mc = controller ?: return
        if (mc.currentPosition > 3000L) {
            mc.seekTo(0L)
        } else {
            mc.seekToPrevious()
        }
    }

    fun jumpToMarker(marker: TrackMarkerEntity) {
        _selectedMarker.value = marker
        seekTo(marker.timestampMs)
        val mc = controller
        if (mc != null && !mc.isPlaying) {
            mc.play()
        }
    }

    fun clearSelectedMarker() {
        _selectedMarker.value = null
    }

    fun addMarker(timestampMs: Long, comment: String, colorHex: String) {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            repository.addMarker(
                trackHash = track.trackHash,
                timestampMs = timestampMs,
                comment = comment.ifBlank { "Penanda" },
                colorHex = colorHex
            )
            _statusMessage.value = "Penanda disimpan pada ${formatTimeMs(timestampMs)}"
        }
    }

    fun updateMarker(marker: TrackMarkerEntity, newComment: String, newTimestampMs: Long, newColorHex: String) {
        viewModelScope.launch {
            val updated = marker.copy(
                comment = newComment.ifBlank { "Penanda" },
                timestampMs = newTimestampMs,
                colorHex = newColorHex
            )
            repository.updateMarker(updated)
            if (_selectedMarker.value?.id == marker.id) {
                _selectedMarker.value = updated
            }
        }
    }

    fun deleteMarker(marker: TrackMarkerEntity) {
        viewModelScope.launch {
            repository.deleteMarker(marker)
            if (_selectedMarker.value?.id == marker.id) {
                _selectedMarker.value = null
            }
        }
    }

    fun saveTranscriptText(rawText: String) {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            repository.saveTranscriptFromRaw(track.trackHash, rawText)
            _statusMessage.value = "Transkrip berhasil disimpan"
        }
    }

    fun syncTranscriptLineToCurrentTime(line: TrackTranscriptEntity) {
        val nowMs = _currentPositionMs.value
        viewModelScope.launch {
            repository.syncTranscriptLine(line.id, nowMs)
        }
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
                _statusMessage.value = "${parsed.size} penanda berhasil diimpor"
            }.onFailure {
                _statusMessage.value = "Format JSON penanda tidak valid"
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
        it.copy(
            eqEnabled = true,
            eqPreset = preset,
            eqBandLevelsDb = preset.bandsDb
        )
    }

    fun setEqBand(index: Int, db: Int) = updateEffects { state ->
        val mutable = state.eqBandLevelsDb.toMutableList()
        if (index in mutable.indices) {
            mutable[index] = db.coerceIn(-10, 10)
        }
        state.copy(
            eqEnabled = true,
            eqPreset = EqPreset.CUSTOM,
            eqBandLevelsDb = mutable
        )
    }

    fun setBassBoost(strength: Int) = updateEffects {
        it.copy(bassBoostStrength = strength.coerceIn(0, 1000))
    }

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
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
