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
import com.notara.player.ui.AbLoopState
import kotlin.math.abs

@Composable
fun WaveformSeekBar(
    amplitudes: List<Float>,
    positionMs: Long,
    durationMs: Long,
    markers: List<TrackMarkerEntity>,
    selectedMarkerId: Long?,
    abLoop: AbLoopState = AbLoopState(),
    onSeek: (Long) -> Unit,
    onMarkerTap: (TrackMarkerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val playedColor = MaterialTheme.colorScheme.primary
    val unplayedColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f)
    val playheadColor = MaterialTheme.colorScheme.onBackground
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val bars = if (amplitudes.isNotEmpty()) amplitudes else List(64) { 0.35f }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .pointerInput(markers, safeDuration) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
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
        val topPadding = 22f
        val waveHeight = h - topPadding - 6f
        val centerY = topPadding + waveHeight / 2f

        // Sorotan area A-B Loop jika aktif
        if (abLoop.startMs != null) {
            val sx = ((abLoop.startMs.toFloat() / safeDuration).coerceIn(0f, 1f)) * w
            if (abLoop.endMs != null && abLoop.endMs > abLoop.startMs) {
                val ex = ((abLoop.endMs.toFloat() / safeDuration).coerceIn(0f, 1f)) * w
                drawRoundRect(
                    color = playedColor.copy(alpha = 0.16f),
                    topLeft = Offset(sx, topPadding - 4f),
                    size = Size((ex - sx).coerceAtLeast(4f), waveHeight + 6f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            } else {
                drawLine(
                    color = playedColor,
                    start = Offset(sx, topPadding - 6f),
                    end = Offset(sx, h - 2f),
                    strokeWidth = 3f
                )
            }
        }

        val slotWidth = w / bars.size
        val barWidth = (slotWidth * 0.58f).coerceAtLeast(2f)

        bars.forEachIndexed { index, amp ->
            val x = index * slotWidth + (slotWidth - barWidth) / 2f
            val barH = (amp * waveHeight).coerceAtLeast(5f)
            val isPlayed = (index.toFloat() / bars.size) <= progress
            drawRoundRect(
                color = if (isPlayed) playedColor else unplayedColor,
                topLeft = Offset(x, centerY - barH / 2f),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        val playheadX = progress * w
        drawLine(
            color = playheadColor.copy(alpha = 0.85f),
            start = Offset(playheadX, topPadding - 2f),
            end = Offset(playheadX, h - 4f),
            strokeWidth = 2.5f
        )

        markers.forEach { marker ->
            val mx = ((marker.timestampMs.toFloat() / safeDuration).coerceIn(0f, 1f)) * w
            val pinColor = parseHexColor(marker.colorHex)
            val isSelected = marker.id == selectedMarkerId
            val headRadius = if (isSelected) 9f else 6.5f

            drawLine(
                color = pinColor,
                start = Offset(mx, 12f),
                end = Offset(mx, h - 4f),
                strokeWidth = if (isSelected) 3f else 2f
            )
            if (isSelected) {
                drawCircle(color = pinColor.copy(alpha = 0.28f), radius = 15f, center = Offset(mx, 11f))
            }
            drawCircle(color = pinColor, radius = headRadius, center = Offset(mx, 11f))
            if (isSelected) {
                drawCircle(color = Color.White, radius = 3f, center = Offset(mx, 11f))
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(Color(0xFFE8A864))
}
