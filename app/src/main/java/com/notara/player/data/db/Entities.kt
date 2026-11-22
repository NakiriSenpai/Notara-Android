package com.notara.player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_markers",
    indices = [Index(value = ["trackHash"])]
)
data class TrackMarkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackHash: String,
    val timestampMs: Long,
    val comment: String,
    val colorHex: String = "#F59E0B",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "track_transcripts",
    indices = [Index(value = ["trackHash"])]
)
data class TrackTranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackHash: String,
    val lineIndex: Int,
    val startMs: Long?, // null jika belum disinkronkan
    val text: String
)

@Entity(tableName = "track_waveforms")
data class TrackWaveformEntity(
    @PrimaryKey val trackHash: String,
    val amplitudesCsv: String
)
