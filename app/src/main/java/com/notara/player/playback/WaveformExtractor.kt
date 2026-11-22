package com.notara.player.playback

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class WaveformExtractor(private val context: Context) {

    /**
     * Mengekstrak 68 batang waveform ASLI mengikuti dinamika keras-pelannya musik
     * menggunakan MediaExtractor + MediaCodec (sample PCM 16-bit RMS di 68 titik waktu).
     */
    suspend fun extractBars(
        uri: Uri,
        seedKey: String,
        barCount: Int = 68
    ): List<Float> = withContext(Dispatchers.IO) {
        val decoded = runCatching { decodeRealRmsBars(uri, barCount) }.getOrNull()
        decoded ?: List(barCount) { 0.25f }
    }

    private fun decodeRealRmsBars(uri: Uri, barCount: Int): List<Float>? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return null

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            if (durationUs <= 0L) return null

            extractor.selectTrack(audioTrackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val rmsValues = FloatArray(barCount)
            val bufferInfo = MediaCodec.BufferInfo()
            val stepUs = durationUs / barCount

            for (i in 0 until barCount) {
                val targetUs = (i * stepUs + stepUs / 2L).coerceIn(0L, durationUs - 1L)
                extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                codec.flush()

                var sumSquares = 0.0
                var sampleCount = 0
                var framesDecoded = 0

                // Decode hingga 3 frame PCM pada titik waktu tersebut (sangat cepat, ~2-4 ms per titik)
                for (attempt in 0 until 8) {
                    val inIdx = codec.dequeueInputBuffer(8_000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize > 0) {
                                codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            } else {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 8_000L)
                    if (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = outBuf.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val n = shortBuf.remaining()
                            // Sub-sample tiap 2 sample agar makin hemat CPU
                            var k = 0
                            while (k < n) {
                                val s = shortBuf.get(k).toDouble() / 32768.0
                                sumSquares += s * s
                                sampleCount++
                                k += 2
                            }
                            framesDecoded++
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if (framesDecoded >= 2) break
                    }
                }

                rmsValues[i] = if (sampleCount > 0) {
                    sqrt(sumSquares / sampleCount).toFloat()
                } else {
                    0f
                }
            }

            // Normalisasi berbasis persentil atas agar bagian tenang (intro) & reff (keras) kontras nyata!
            val sorted = rmsValues.sorted()
            val p95 = sorted[(sorted.size * 95 / 100).coerceIn(0, sorted.lastIndex)]
            val minV = sorted[(sorted.size * 5 / 100).coerceIn(0, sorted.lastIndex)]
            val range = max(0.0001f, p95 - minV)

            return List(barCount) { idx ->
                val norm = ((rmsValues[idx] - minV) / range).coerceIn(0f, 1f)
                // Kurva kontras eksponensial ringan agar dinamika musik terlihat jelas
                val shaped = norm.pow(1.25f)
                (0.10f + shaped * 0.90f).coerceIn(0.10f, 1.0f)
            }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
