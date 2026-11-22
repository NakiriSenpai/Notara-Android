package com.notara.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.notara.player.data.model.LocalTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LocalAudioScanner(private val context: Context) {

    suspend fun scanAllAudio(): List<LocalTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalTrack>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("3000") // Abaikan suara notifikasi < 3 detik
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Tanpa Judul"
                val rawArtist = cursor.getString(artistCol)
                val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") {
                    "Artis Tidak Diketahui"
                } else {
                    rawArtist
                }
                val album = cursor.getString(albumCol) ?: "Album Lokal"
                val albumId = cursor.getLong(albumIdCol)
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val displayName = cursor.getString(nameCol) ?: title

                val rawPath = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                val folderName = extractFolderName(rawPath)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val albumArtUri = if (albumId > 0) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                } else {
                    null
                }

                // Hash stabil berbasis metadata inti agar penanda bertahan meski file dipindah folder
                val trackHash = stableTrackHash(displayName, size, duration)

                tracks.add(
                    LocalTrack(
                        mediaStoreId = id,
                        trackHash = trackHash,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        folderName = folderName,
                        sizeBytes = size
                    )
                )
            }
        }
        tracks
    }

    private fun extractFolderName(rawPath: String): String {
        val cleaned = rawPath.trim('/').replace("\\", "/")
        if (cleaned.isEmpty()) return "Penyimpanan Utama"
        val segments = cleaned.split("/").filter { it.isNotBlank() }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            segments.lastOrNull() ?: "Penyimpanan Utama"
        } else {
            if (segments.size >= 2) segments[segments.size - 2] else segments.first()
        }
    }

    private fun stableTrackHash(displayName: String, sizeBytes: Long, durationMs: Long): String {
        val key = "${displayName.lowercase()}|$sizeBytes|$durationMs"
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
