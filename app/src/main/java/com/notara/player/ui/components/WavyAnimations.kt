package com.notara.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notara.player.ui.formatTimeMs
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Logo Notara: Batang waveform yang membentuk siluet tegas huruf "N" + titik pin di kanan atas.
 */
@Composable
fun NotaraBrandIcon(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.primary
    val fg = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = bg,
            cornerRadius = CornerRadius(w * 0.28f, w * 0.28f)
        )

        // (topFrac, bottomFrac) membentuk huruf N secara tegas!
        val bars = listOf(
            0.26f to 0.78f, // Pilar kiri N
            0.31f to 0.56f, // Diagonal 1
            0.40f to 0.64f, // Diagonal 2
            0.48f to 0.71f, // Diagonal 3
            0.55f to 0.78f, // Diagonal 4
            0.34f to 0.78f  // Pilar kanan N
        )
        val barW = w * 0.088f
        val gap = w * 0.044f
        val totalW = bars.size * barW + (bars.size - 1) * gap
        val startX = (w - totalW) / 2f

        bars.forEachIndexed { idx, (topF, botF) ->
            val x = startX + idx * (barW + gap)
            val topY = h * topF
            val barH = h * (botF - topF)
            drawRoundRect(
                color = fg,
                topLeft = Offset(x, topY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
        // Titik pin di atas pilar kanan N
        val pinX = startX + 5 * (barW + gap) + barW / 2f
        drawCircle(color = fg, radius = w * 0.068f, center = Offset(pinX, h * 0.21f))
    }
}

/**
 * Lingkaran progres bergelombang HALUS ORGANIK (Material 3 Expressive).
 * Menggunakan 200 titik resolusi tinggi + quadratic smoothing & StrokeJoin.Round
 * sehingga licin sempurna seperti kelopak gelombang, tidak patah-patah.
 */
@Composable
fun WavyCircularProgressRing(
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    isPlaying: Boolean,
    ringSize: Dp = 54.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pos by positionMsFlow.collectAsStateWithLifecycle()
    val progress = (pos.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0.03f, 1f)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)

    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "wavy_circle")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        p
    } else {
        0f
    }

    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        content()
        Canvas(modifier = Modifier.size(ringSize)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val strokeW = 2.8.dp.toPx()
            val waveAmp = if (isPlaying) 1.55.dp.toPx() else 0.75.dp.toPx()
            val baseRadius = size.minDimension / 2f - strokeW - waveAmp - 1f

            // Track lingkaran halus di belakang
            drawCircle(
                color = track,
                radius = baseRadius,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW * 0.7f)
            )

            // 200 titik resolusi tinggi agar setiap lengkungan gelombang benar-benar licin!
            val sweepRad = (progress * 2.0 * PI).toFloat()
            val startRad = (-PI / 2.0).toFloat()
            val steps = (progress * 200f).toInt().coerceIn(24, 220)
            val waveFreq = 12f

            val path = Path()
            var prevX = 0f
            var prevY = 0f
            for (i in 0..steps) {
                val frac = i.toFloat() / steps.toFloat()
                val angle = startRad + frac * sweepRad
                // Taper lembut di ujung awal & akhir agar sambungan gelombang halus
                val edgeTaper = sin(frac * PI).toFloat().coerceIn(0.15f, 1f)
                val r = baseRadius + (waveAmp * edgeTaper) * sin(angle * waveFreq + phase)
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    val midX = (prevX + x) * 0.5f
                    val midY = (prevY + y) * 0.5f
                    path.quadraticTo(prevX, prevY, midX, midY)
                }
                prevX = x
                prevY = y
            }
            path.lineTo(prevX, prevY)

            drawPath(
                path = path,
                color = accent,
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun MiniWaveformProgressBar(
    amplitudes: List<Float>,
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    isActiveTrack: Boolean,
    isPlaying: Boolean,
    onSeek: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rawPos by positionMsFlow.collectAsStateWithLifecycle()
    val displayPos = if (isActiveTrack) rawPos else 0L
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (displayPos.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val playedColor = MaterialTheme.colorScheme.primary
    val unplayedColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.36f)

    val bars = remember(amplitudes, isActiveTrack) {
        val targetCount = 36
        if (isActiveTrack && amplitudes.size >= targetCount) {
            val step = amplitudes.size.toFloat() / targetCount
            List(targetCount) { i ->
                val idx = (i * step).toInt().coerceIn(0, amplitudes.lastIndex)
                amplitudes[idx].coerceIn(0.16f, 1.0f)
            }
        } else {
            List(targetCount) { i ->
                val env = sin((i.toFloat() / (targetCount - 1)) * PI).toFloat()
                val ripple = 0.55f + 0.45f * sin(i * 0.85f)
                (0.20f + env * ripple * 0.78f).coerceIn(0.18f, 0.96f)
            }
        }
    }

    val pulse = if (isActiveTrack && isPlaying) {
        val transition = rememberInfiniteTransition(label = "wf_pulse")
        val p by transition.animateFloat(
            initialValue = 0.84f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(420, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        p
    } else {
        1.0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .then(
                    if (onSeek != null && isActiveTrack) {
                        Modifier.pointerInput(safeDuration) {
                            detectTapGestures { offset ->
                                val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                onSeek((frac * safeDuration).toLong())
                            }
                        }
                    } else Modifier
                )
        ) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val slotW = w / bars.size
            val barW = (slotW * 0.56f).coerceAtLeast(2.4.dp.toPx())
            val activeIdx = (progress * (bars.size - 1)).toInt().coerceIn(0, bars.lastIndex)

            bars.forEachIndexed { idx, amp ->
                val isPlayed = isActiveTrack && (idx.toFloat() / bars.size) <= progress
                val boost = if (isActiveTrack && isPlaying && (idx == activeIdx || idx == activeIdx - 1)) pulse else 1f
                val barH = (amp * h * 0.92f * boost).coerceIn(5.dp.toPx(), h)
                val x = idx * slotW + (slotW - barW) / 2f

                drawRoundRect(
                    color = if (isPlayed) playedColor else unplayedColor,
                    topLeft = Offset(x, centerY - barH / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTimeMs(displayPos), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTimeMs(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
