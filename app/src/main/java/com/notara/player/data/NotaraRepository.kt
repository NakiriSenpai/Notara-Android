package com.notara.player.data

import com.notara.player.data.db.NotaraDao
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.db.PlaylistTrackCrossRef
import com.notara.player.data.db.TrackCustomCoverEntity
import com.notara.player.data.db.TrackMarkerEntity
import com.notara.player.data.db.TrackTranscriptEntity
import com.notara.player.data.db.TrackWaveformEntity
import com.notara.player.data.model.LocalTrack
import kotlinx.coroutines.flow.Flow

class NotaraRepository(
    private val scanner: LocalAudioScanner,
    private val dao: NotaraDao
) {
    suspend fun loadLocalTracks(): List<LocalTrack> = scanner.scanAllAudio()

    // --- Markers ---
    fun observeMarkers(trackHash: String): Flow<List<TrackMarkerEntity>> =
        dao.observeMarkers(trackHash)

    suspend fun getMarkersOnce(trackHash: String): List<TrackMarkerEntity> =
        dao.getMarkersOnce(trackHash)

    suspend fun addMarker(
        trackHash: String,
        timestampMs: Long,
        title: String,
        description: String,
        colorHex: String
    ) {
        dao.insertMarker(
            TrackMarkerEntity(
                trackHash = trackHash,
                timestampMs = timestampMs,
                title = title.trim().ifBlank { "Penanda" },
                description = description.trim(),
                colorHex = colorHex
            )
        )
    }

    suspend fun importMarkers(markers: List<TrackMarkerEntity>) {
        if (markers.isNotEmpty()) dao.insertMarkers(markers)
    }

    suspend fun updateMarker(marker: TrackMarkerEntity) = dao.updateMarker(marker)

    suspend fun deleteMarker(marker: TrackMarkerEntity) = dao.deleteMarker(marker)

    // --- Transcripts ---
    fun observeTranscript(trackHash: String): Flow<List<TrackTranscriptEntity>> =
        dao.observeTranscript(trackHash)

    suspend fun getTranscriptOnce(trackHash: String): List<TrackTranscriptEntity> =
        dao.getTranscriptOnce(trackHash)

    suspend fun saveTranscriptFromRaw(trackHash: String, rawText: String) {
        val parsed = TranscriptParser.parse(rawText, trackHash)
        dao.replaceTranscript(trackHash, parsed)
    }

    suspend fun syncTranscriptLine(lineId: Long, timestampMs: Long) {
        dao.updateTranscriptTimestamp(lineId, timestampMs)
    }

    // --- Waveforms ---
    suspend fun getCachedWaveform(trackHash: String): List<Float>? {
        val entity = dao.getWaveform(trackHash) ?: return null
        return entity.amplitudesCsv
            .split(",")
            .mapNotNull { it.toFloatOrNull() }
            .takeIf { it.isNotEmpty() }
    }

    suspend fun saveCachedWaveform(trackHash: String, amplitudes: List<Float>) {
        if (amplitudes.isEmpty()) return
        val csv = amplitudes.joinToString(",") { "%.3f".format(it) }
        dao.saveWaveform(TrackWaveformEntity(trackHash = trackHash, amplitudesCsv = csv))
    }

    // --- Playlists ---
    fun observePlaylists(): Flow<List<PlaylistEntity>> = dao.observePlaylists()

    fun observeAllPlaylistRefs(): Flow<List<PlaylistTrackCrossRef>> = dao.observeAllPlaylistRefs()

    suspend fun createPlaylist(name: String, coverPath: String?): Long {
        return dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim().ifBlank { "Playlist Baru" },
                customCoverPath = coverPath
            )
        )
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) = dao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlistId: Long) {
        dao.clearPlaylistTracks(playlistId)
        dao.deletePlaylistById(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackHash: String) {
        dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackHash = trackHash))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackHash: String) {
        dao.removeTrackFromPlaylist(playlistId, trackHash)
    }

    // --- Custom Track Covers ---
    fun observeAllCustomCovers(): Flow<List<TrackCustomCoverEntity>> = dao.observeAllCustomCovers()

    suspend fun setTrackCustomCover(trackHash: String, coverPath: String) {
        dao.saveTrackCustomCover(TrackCustomCoverEntity(trackHash = trackHash, coverPath = coverPath))
    }

    suspend fun clearTrackCustomCover(trackHash: String) {
        dao.deleteTrackCustomCover(trackHash)
    }
}
