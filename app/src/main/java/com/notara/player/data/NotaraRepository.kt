package com.notara.player.data

import com.notara.player.data.db.NotaraDao
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

    fun observeMarkers(trackHash: String): Flow<List<TrackMarkerEntity>> =
        dao.observeMarkers(trackHash)

    suspend fun getMarkersOnce(trackHash: String): List<TrackMarkerEntity> =
        dao.getMarkersOnce(trackHash)

    suspend fun addMarker(
        trackHash: String,
        timestampMs: Long,
        comment: String,
        colorHex: String
    ) {
        dao.insertMarker(
            TrackMarkerEntity(
                trackHash = trackHash,
                timestampMs = timestampMs,
                comment = comment.trim(),
                colorHex = colorHex
            )
        )
    }

    suspend fun importMarkers(markers: List<TrackMarkerEntity>) {
        if (markers.isNotEmpty()) dao.insertMarkers(markers)
    }

    suspend fun updateMarker(marker: TrackMarkerEntity) = dao.updateMarker(marker)

    suspend fun deleteMarker(marker: TrackMarkerEntity) = dao.deleteMarker(marker)

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
}
