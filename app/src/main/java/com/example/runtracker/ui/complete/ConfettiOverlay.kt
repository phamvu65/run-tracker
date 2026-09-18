package com.example.runtracker.ui.complete

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private class ConfettiPiece(
    val startXFraction: Float,
    val fallDelay: Float,
    val driftAmplitude: Float,
    val driftPhase: Float,
    val rotationSpeed: Float,
    val sizeDp: Float,
    val colorIndex: Int,
)

/**
 * Pháo hoa giấy rơi một lần khi vào màn kết quả — không lặp lại, tự ẩn khi rơi hết (không chặn
 * chạm vì Canvas thuần không gắn pointerInput). Vẽ đè lên nội dung nên phải là lớp SAU CÙNG
 * trong Box chứa nó.
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier, pieceCount: Int = 28) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFFFBE4C),
    )
    val pieces = remember {
        val rnd = Random(System.nanoTime())
        List(pieceCount) {
            ConfettiPiece(
                startXFraction = rnd.nextFloat(),
                fallDelay = rnd.nextFloat() * 0.25f,
                driftAmplitude = 14f + rnd.nextFloat() * 20f,
                driftPhase = rnd.nextFloat() * 6.28f,
                rotationSpeed = 180f + rnd.nextFloat() * 360f,
                sizeDp = 5f + rnd.nextFloat() * 4f,
                colorIndex = rnd.nextInt(colors.size),
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(1700, easing = LinearEasing))
    }

    Canvas(modifier) {
        val h = size.height
        val w = size.width
        pieces.forEach { p ->
            val t = ((progress.value - p.fallDelay) / (1f - p.fallDelay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val y = -40f + t * (h + 80f)
            val x = p.startXFraction * w + sin(p.driftPhase + t * 8f) * p.driftAmplitude
            val alpha = if (t > 0.85f) ((1f - t) / 0.15f).coerceIn(0f, 1f) else 1f
            val sizePx = p.sizeDp * density
            rotate(degrees = t * p.rotationSpeed, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[p.colorIndex].copy(alpha = alpha),
                    topLeft = Offset(x - sizePx / 2f, y - sizePx * 0.8f),
                    size = Size(sizePx, sizePx * 1.6f),
                )
            }
        }
    }
}
