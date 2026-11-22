package com.notara.player.data

import android.net.Uri
import com.notara.player.data.db.CachedTrackEntity
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
    suspend fun loadCachedTracks(): List<LocalTrack> {
        return dao.getCachedTracks().map { e ->
            LocalTrack(
                mediaStoreId = e.mediaStoreId,
                trackHash = e.trackHash,
                contentUri = Uri.parse(e.contentUriString),
                albumArtUri = e.albumArtUriString?.let { Uri.parse(it) },
                title = e.title,
                artist = e.artist,
                album = e.album,
                durationMs = e.durationMs,
                folderName = e.folderName,
                sizeBytes = e.sizeBytes
            )
        }
    }

    suspend fun scanAndCacheLocalTracks(): List<LocalTrack> {
        val scanned = scanner.scanAllAudio()
        val entities = scanned.mapIndexed { idx, t ->
            CachedTrackEntity(
                mediaStoreId = t.mediaStoreId,
                trackHash = t.trackHash,
                contentUriString = t.contentUri.toString(),
                albumArtUriString = t.albumArtUri?.toString(),
                title = t.title,
                artist = t.artist,
                album = t.album,
                durationMs = t.durationMs,
                folderName = t.folderName,
                sizeBytes = t.sizeBytes,
                sortOrder = idx
            )
        }
        dao.replaceCachedTracks(entities)
        return scanned
    }

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
                title = title.trim().ifBlank { "Pin" },
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
            .takeIf { it.size >= 32 }
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
                name = name.trim().ifBlank { "Playlist" },
                customCoverPath = coverPath
            )
        )
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) = dao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlistId: Long) {
        dao.clearPlaylistTracks(playlistId)
        dao.deletePlaylistById(playlistId)
    }

    /**
     * Mengembalikan false jika lagu sudah ada di playlist (mencegah duplikat!).
     */
    suspend fun addTrackToPlaylistIfAbsent(playlistId: Long, trackHash: String): Boolean {
        if (dao.countTrackInPlaylist(playlistId, trackHash) > 0) {
            return false
        }
        dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackHash = trackHash))
        return true
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
