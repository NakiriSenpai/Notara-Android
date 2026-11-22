package com.notara.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CoverImageStorage(private val context: Context) {

    private val coversDir: File
        get() = File(context.filesDir, "custom_covers").apply { if (!exists()) mkdirs() }

    /**
     * Menyalin dan mengecilkan foto pilihan pengguna (maks 512px) ke penyimpanan internal
     * agar ringan di HP spek rendah dan tidak kehilangan izin URI setelah restart.
     */
    suspend fun saveCustomCover(sourceUri: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val rawBitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@runCatching null

            val maxSide = 512
            val w = rawBitmap.width
            val h = rawBitmap.height
            val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h).coerceAtLeast(1))
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }

            val outFile = File(coversDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 86, fos)
            }
            if (scaled !== rawBitmap) scaled.recycle()
            rawBitmap.recycle()
            outFile.absolutePath
        }.getOrNull()
    }
}
