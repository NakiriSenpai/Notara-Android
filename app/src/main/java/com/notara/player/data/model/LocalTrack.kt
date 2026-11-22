package com.notara.player.data.model

import android.net.Uri

data class LocalTrack(
    val mediaStoreId: Long,
    val trackHash: String,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val folderName: String,
    val sizeBytes: Long
)
