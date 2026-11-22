package com.notara.player.playback

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class WaveformExtractor(private val context: Context) {

    /**
     * Mengekstrak 72 batang amplitudo ternormalisasi (0.14f..1.0f) secara cepat dari berkas lokal.
     * Menggunakan sampling blok biner agar instan untuk file MP3/M4A/FLAC/WAV tanpa membebani CPU.
     */
    suspend fun extractBars(
        uri: Uri,
        seedKey: String,
        barCount: Int = 72
    ): List<Float> = withContext(Dispatchers.IO) {
        val sampled = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.size < barCount * 16) return@use null
                // Lewati header awal (~4 KB) agar lebih mencerminkan isi audio
                val startOffset = minOf(4096, bytes.size / 10)
                val usableLen = bytes.size - startOffset
                val step = max(1, usableLen / barCount)
                val raw = FloatArray(barCount)
                var maxVal = 1f

                for (i in 0 until barCount) {
                    val base = startOffset + i * step
                    val sampleWindow = minOf(256, step, bytes.size - base)
                    var sum = 0L
                    for (j in 0 until sampleWindow step 2) {
                        val b = bytes[base + j].toInt()
                        sum += abs(b)
                    }
                    val avg = if (sampleWindow > 0) sum.toFloat() / (sampleWindow / 2f) else 10f
                    raw[i] = avg
                    if (avg > maxVal) maxVal = avg
                }

                // Smoothing ringan agar tampilan waveform enak dilihat seperti SoundCloud
                List(barCount) { idx ->
                    val prev = raw[max(0, idx - 1)]
                    val curr = raw[idx]
                    val next = raw[minOf(barCount - 1, idx + 1)]
                    val blended = (prev * 0.2f + curr * 0.6f + next * 0.2f) / maxVal
                    (0.16f + blended * 0.84f).coerceIn(0.16f, 1.0f)
                }
            }
        }.getOrNull()

        sampled ?: fallbackWaveform(seedKey, barCount)
    }

    private fun fallbackWaveform(seedKey: String, barCount: Int): List<Float> {
        val rng = Random(seedKey.hashCode())
        return List(barCount) { 0.22f + rng.nextFloat() * 0.72f }
    }
}
