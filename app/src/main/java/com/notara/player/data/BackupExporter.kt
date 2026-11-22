package com.notara.player.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.notara.player.data.db.FavoriteTrackEntity
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.db.PlaylistTrackCrossRef
import com.notara.player.data.db.TrackMarkerEntity
import com.notara.player.data.db.TrackTranscriptEntity

data class MarkerBackupPayload(
    val app: String = "Notara",
    val version: Int = 2,
    val trackTitle: String,
    val trackHash: String,
    val markers: List<MarkerBackupItem>
)

data class MarkerBackupItem(
    val timestampMs: Long,
    val title: String? = null,
    val description: String? = null,
    val comment: String? = null,
    val colorHex: String = "#E8A864"
)

data class FullBackupPlaylist(
    val name: String,
    val trackHashes: List<String>
)

data class FullNotaraBackup(
    val app: String = "Notara",
    val backupVersion: Int = 4,
    val exportedAt: Long = System.currentTimeMillis(),
    val favoriteHashes: List<String> = emptyList(),
    val playlists: List<FullBackupPlaylist> = emptyList(),
    val markers: List<TrackMarkerEntity> = emptyList(),
    val transcripts: List<TrackTranscriptEntity> = emptyList(),
    val hiddenFolders: List<String> = emptyList(),
    val minDurationSec: Int = 3
)

object BackupExporter {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportMarkersToJson(
        trackTitle: String,
        trackHash: String,
        markers: List<TrackMarkerEntity>
    ): String {
        val payload = MarkerBackupPayload(
            trackTitle = trackTitle,
            trackHash = trackHash,
            markers = markers.map {
                MarkerBackupItem(
                    timestampMs = it.timestampMs,
                    title = it.title,
                    description = it.description,
                    colorHex = it.colorHex
                )
            }
        )
        return gson.toJson(payload)
    }

    fun importMarkersFromJson(json: String, targetTrackHash: String): List<TrackMarkerEntity> {
        val payload = gson.fromJson(json, MarkerBackupPayload::class.java) ?: return emptyList()
        return payload.markers.map { item ->
            val resolvedTitle = item.title?.takeIf { it.isNotBlank() }
                ?: item.comment?.takeIf { it.isNotBlank() }
                ?: "Pin"
            TrackMarkerEntity(
                trackHash = targetTrackHash,
                timestampMs = item.timestampMs,
                title = resolvedTitle,
                description = item.description ?: "",
                colorHex = item.colorHex.ifBlank { "#E8A864" }
            )
        }
    }

    fun exportFullBackup(
        favorites: List<FavoriteTrackEntity>,
        playlists: List<PlaylistEntity>,
        refs: List<PlaylistTrackCrossRef>,
        markers: List<TrackMarkerEntity>,
        transcripts: List<TrackTranscriptEntity>,
        filterConfig: LibraryFilterConfig
    ): String {
        val refMap = refs.groupBy({ it.playlistId }, { it.trackHash })
        val full = FullNotaraBackup(
            favoriteHashes = favorites.map { it.trackHash },
            playlists = playlists.map { pl ->
                FullBackupPlaylist(name = pl.name, trackHashes = refMap[pl.id].orEmpty())
            },
            markers = markers.map { it.copy(id = 0) },
            transcripts = transcripts.map { it.copy(id = 0) },
            hiddenFolders = filterConfig.hiddenFolders.toList(),
            minDurationSec = filterConfig.minDurationSec
        )
        return gson.toJson(full)
    }

    fun parseFullBackup(json: String): FullNotaraBackup? {
        return runCatching { gson.fromJson(json, FullNotaraBackup::class.java) }.getOrNull()
    }
}
