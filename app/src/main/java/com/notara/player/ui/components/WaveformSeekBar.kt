package com.notara.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.notara.player.data.db.TrackMarkerEntity
import kotlin.math.abs

@Composable
fun WaveformSeekBar(
    amplitudes: List<Float>,
    positionMs: Long,
    durationMs: Long,
    markers: List<TrackMarkerEntity>,
    selectedMarkerId: Long?,
    onSeek: (Long) -> Unit,
    onMarkerTap: (TrackMarkerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val playedColor = MaterialTheme.colorScheme.primary
    val unplayedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val playheadColor = MaterialTheme.colorScheme.onBackground
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val bars = if (amplitudes.isNotEmpty()) amplitudes else List(64) { 0.35f }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .pointerInput(markers, safeDuration) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    // Cek apakah tap mengenai pin penanda (toleransi 26 px)
                    val hitMarker = markers.minByOrNull { marker ->
                        val mx = (marker.timestampMs.toFloat() / safeDuration) * width
                        abs(offset.x - mx)
                    }
                    if (hitMarker != null) {
                        val mx = (hitMarker.timestampMs.toFloat() / safeDuration) * width
                        if (abs(offset.x - mx) <= 28f) {
                            onMarkerTap(hitMarker)
                            return@detectTapGestures
                        }
                    }
                    val fraction = (offset.x / width).coerceIn(0f, 1f)
                    onSeek((fraction * safeDuration).toLong())
                }
            }
            .pointerInput(safeDuration) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val fraction = (change.position.x / width).coerceIn(0f, 1f)
                    onSeek((fraction * safeDuration).toLong())
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val topPadding = 24f // Ruang untuk kepala pin ala SoundCloud
        val waveHeight = h - topPadding - 8f
        val centerY = topPadding + waveHeight / 2f

        val slotWidth = w / bars.size
        val barWidth = (slotWidth * 0.62f).coerceAtLeast(2f)

        // 1. Gambar batang waveform
        bars.forEachIndexed { index, amp ->
            val x = index * slotWidth + (slotWidth - barWidth) / 2f
            val barH = (amp * waveHeight).coerceAtLeast(6f)
            val isPlayed = (index.toFloat() / bars.size) <= progress
            drawRoundRect(
                color = if (isPlayed) playedColor else unplayedColor,
                topLeft = Offset(x, centerY - barH / 2f),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        // 2. Gambar garis playhead aktif
        val playheadX = progress * w
        drawLine(
            color = playheadColor,
            start = Offset(playheadX, topPadding - 4f),
            end = Offset(playheadX, h - 4f),
            strokeWidth = 3f
        )

        // 3. Gambar pin penanda waktu ala SoundCloud
        markers.forEach { marker ->
            val mx = ((marker.timestampMs.toFloat() / safeDuration).coerceIn(0f, 1f)) * w
            val pinColor = parseHexColor(marker.colorHex)
            val isSelected = marker.id == selectedMarkerId
            val headRadius = if (isSelected) 9.5f else 7f

            drawLine(
                color = pinColor,
                start = Offset(mx, 14f),
                end = Offset(mx, h - 6f),
                strokeWidth = if (isSelected) 3.5f else 2.2f
            )
            drawCircle(
                color = pinColor,
                radius = headRadius,
                center = Offset(mx, 12f)
            )
            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = 3.2f,
                    center = Offset(mx, 12f)
                )
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(Color(0xFFF59E0B))
}
