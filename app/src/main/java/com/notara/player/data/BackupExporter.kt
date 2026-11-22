package com.notara.player.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.notara.player.data.db.TrackMarkerEntity

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
                ?: "Penanda"
            val resolvedDesc = item.description ?: ""
            TrackMarkerEntity(
                trackHash = targetTrackHash,
                timestampMs = item.timestampMs,
                title = resolvedTitle,
                description = resolvedDesc,
                colorHex = item.colorHex.ifBlank { "#E8A864" }
            )
        }
    }
}
