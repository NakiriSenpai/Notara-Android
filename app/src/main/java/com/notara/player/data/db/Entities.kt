package com.notara.player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
    @PrimaryKey val mediaStoreId: Long,
    val trackHash: String,
    val contentUriString: String,
    val albumArtUriString: String?,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val folderName: String,
    val sizeBytes: Long,
    val sortOrder: Int
)

@Entity(
    tableName = "track_markers",
    indices = [Index(value = ["trackHash"])]
)
data class TrackMarkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackHash: String,
    val timestampMs: Long,
    val title: String,
    val description: String = "",
    val colorHex: String = "#E8A864",
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
    val startMs: Long?,
    val text: String
)

@Entity(tableName = "track_waveforms")
data class TrackWaveformEntity(
    @PrimaryKey val trackHash: String,
    val amplitudesCsv: String
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val customCoverPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackHash"],
    indices = [Index(value = ["playlistId"]), Index(value = ["trackHash"])]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackHash: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "track_custom_covers")
data class TrackCustomCoverEntity(
    @PrimaryKey val trackHash: String,
    val coverPath: String
)

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val trackHash: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val trackHash: String,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)
