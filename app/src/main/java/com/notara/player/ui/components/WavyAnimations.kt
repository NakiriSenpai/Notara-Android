package com.notara.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Desain 01 (Waveform Pin) untuk header pojok kiri atas halaman Home.
 */
@Composable
fun NotaraBrandIcon(
    size: Dp = 34.dp,
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
        val heights = listOf(0.22f, 0.42f, 0.58f, 0.36f, 0.52f, 0.26f)
        val barW = w * 0.075f
        val gap = w * 0.055f
        val totalW = heights.size * barW + (heights.size - 1) * gap
        val startX = (w - totalW) / 2f
        val centerY = h * 0.54f

        heights.forEachIndexed { idx, frac ->
            val bh = h * frac
            val x = startX + idx * (barW + gap)
            drawRoundRect(
                color = fg,
                topLeft = Offset(x, centerY - bh / 2f),
                size = Size(barW, bh),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
        // Titik pin bercahaya di atas batang ke-5
        val pinX = startX + 4 * (barW + gap) + barW / 2f
        drawCircle(color = fg, radius = w * 0.065f, center = Offset(pinX, h * 0.20f))
    }
}

/**
 * Lingkaran progres bergelombang (Wavy Circular Progress) ala Material 3 Expressive.
 * Animasi hanya aktif saat isPlaying == true sehingga tetap 60fps di HP spek rendah.
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
    val progress = (pos.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0.04f, 1f)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)

    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "wavy_circle")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
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
            val baseRadius = size.minDimension / 2f - 4.5f
            val strokeW = 3.2.dp.toPx()

            // Track lingkaran halus di belakang
            drawCircle(
                color = track,
                radius = baseRadius,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW * 0.72f)
            )

            // Busur progres bergelombang (wavy arc)
            val sweepRad = (progress * 2.0 * PI).toFloat()
            val startRad = (-PI / 2.0).toFloat()
            val steps = (progress * 52f).toInt().coerceIn(8, 56)
            val waveAmp = if (isPlaying) 2.2.dp.toPx() else 1.2.dp.toPx()
            val waveFreq = 10f

            val path = Path()
            for (i in 0..steps) {
                val frac = i.toFloat() / steps.toFloat()
                val angle = startRad + frac * sweepRad
                val r = baseRadius + waveAmp * sin(angle * waveFreq + phase)
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = accent,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Garis progres gelombang horizontal hidup untuk Kartu Hero di halaman Home (tidak flat!).
 */
@Composable
fun WavyLiveProgressBar(
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val pos by positionMsFlow.collectAsStateWithLifecycle()
    val progress = (pos.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0.05f, 1f)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "wavy_line")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "line_phase"
        )
        p
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val w = size.width
        val cy = size.height / 2f
        val playedX = (progress * w).coerceIn(8f, w)
        val strokeW = 2.8.dp.toPx()

        // Garis sisa di sebelah kanan
        if (playedX < w) {
            drawLine(
                color = track,
                start = Offset(playedX, cy),
                end = Offset(w, cy),
                strokeWidth = strokeW * 0.7f,
                cap = StrokeCap.Round
            )
        }

        // Gelombang hidup di bagian yang sudah diputar
        val amp = if (isPlaying) 4.5.dp.toPx() else 2.2.dp.toPx()
        val steps = 40
        val path = Path()
        for (i in 0..steps) {
            val x = (i.toFloat() / steps.toFloat()) * playedX
            val y = cy + amp * sin((x / 24f) + phase)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Titik ujung aktif
        drawCircle(
            color = accent,
            radius = 4.dp.toPx(),
            center = Offset(playedX, cy + amp * sin((playedX / 24f) + phase))
        )
    }
}
