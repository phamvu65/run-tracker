package com.example.runtracker.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** Đường + vùng tô đơn giản cho một chuỗi giá trị (không nhãn trục) — dùng cho xu hướng nhiều kỳ. */
@Composable
fun MiniTrendChart(values: List<Double>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = values.max().coerceAtLeast(1.0)
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        fun y(v: Double) = (size.height - (v / maxV) * size.height).toFloat()

        listOf(0.0, 0.5, 1.0).forEach { f ->
            val gy = (size.height - f * size.height).toFloat()
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }

        val linePath = Path()
        val fillPath = Path().apply { moveTo(0f, size.height) }
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val yy = y(v)
            if (i == 0) linePath.moveTo(x, yy) else linePath.lineTo(x, yy)
            fillPath.lineTo(x, yy)
        }
        fillPath.lineTo((values.size - 1) * stepX, size.height)
        fillPath.close()
        drawPath(fillPath, color = fill)
        drawPath(linePath, color = line, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        values.forEachIndexed { i, v ->
            drawCircle(line, radius = 4f, center = Offset(x = i * stepX, y = y(v)))
        }
    }
}
