package com.notara.player.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaraDao {

    // --- Markers ---
    @Query("SELECT * FROM track_markers WHERE trackHash = :trackHash ORDER BY timestampMs ASC")
    fun observeMarkers(trackHash: String): Flow<List<TrackMarkerEntity>>

    @Query("SELECT * FROM track_markers WHERE trackHash = :trackHash ORDER BY timestampMs ASC")
    suspend fun getMarkersOnce(trackHash: String): List<TrackMarkerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: TrackMarkerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarkers(markers: List<TrackMarkerEntity>)

    @Update
    suspend fun updateMarker(marker: TrackMarkerEntity)

    @Delete
    suspend fun deleteMarker(marker: TrackMarkerEntity)

    // --- Transcripts ---
    @Query("SELECT * FROM track_transcripts WHERE trackHash = :trackHash ORDER BY lineIndex ASC")
    fun observeTranscript(trackHash: String): Flow<List<TrackTranscriptEntity>>

    @Query("SELECT * FROM track_transcripts WHERE trackHash = :trackHash ORDER BY lineIndex ASC")
    suspend fun getTranscriptOnce(trackHash: String): List<TrackTranscriptEntity>

    @Query("DELETE FROM track_transcripts WHERE trackHash = :trackHash")
    suspend fun clearTranscript(trackHash: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptLines(lines: List<TrackTranscriptEntity>)

    @Query("UPDATE track_transcripts SET startMs = :startMs WHERE id = :id")
    suspend fun updateTranscriptTimestamp(id: Long, startMs: Long)

    @Transaction
    suspend fun replaceTranscript(trackHash: String, lines: List<TrackTranscriptEntity>) {
        clearTranscript(trackHash)
        if (lines.isNotEmpty()) {
            insertTranscriptLines(lines)
        }
    }

    // --- Waveforms ---
    @Query("SELECT * FROM track_waveforms WHERE trackHash = :trackHash LIMIT 1")
    suspend fun getWaveform(trackHash: String): TrackWaveformEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWaveform(entity: TrackWaveformEntity)
}
