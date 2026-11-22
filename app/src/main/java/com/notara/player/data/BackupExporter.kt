package com.notara.player.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.notara.player.data.db.TrackMarkerEntity

data class MarkerBackupPayload(
    val app: String = "Notara",
    val version: Int = 1,
    val trackTitle: String,
    val trackHash: String,
    val markers: List<MarkerBackupItem>
)

data class MarkerBackupItem(
    val timestampMs: Long,
    val comment: String,
    val colorHex: String
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
                    comment = it.comment,
                    colorHex = it.colorHex
                )
            }
        )
        return gson.toJson(payload)
    }

    fun importMarkersFromJson(json: String, targetTrackHash: String): List<TrackMarkerEntity> {
        val payload = gson.fromJson(json, MarkerBackupPayload::class.java) ?: return emptyList()
        return payload.markers.map {
            TrackMarkerEntity(
                trackHash = targetTrackHash,
                timestampMs = it.timestampMs,
                comment = it.comment,
                colorHex = it.colorHex.ifBlank { "#F59E0B" }
            )
        }
    }
}
