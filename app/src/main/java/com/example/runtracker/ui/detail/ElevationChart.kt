package com.example.runtracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.tracking.RouteStats
import kotlin.math.roundToInt

/**
 * Elevation profile: độ cao (y) theo quãng đường tích luỹ (x). Một chuỗi dữ liệu —
 * area mờ + đường viền, không cần legend (tiêu đề đã nói rõ). Ẩn nếu thiết bị không
 * cấp dữ liệu độ cao (mọi altitude = 0).
 */
@Composable
fun ElevationChart(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val altitudes = remember(points) { points.map { it.altitude } }
    if (points.size < 2 || altitudes.all { it == 0.0 }) return

    val distances = remember(points) { RouteStats.cumulativeDistances(points) }
    val minAlt = altitudes.min()
    val maxAlt = altitudes.max()
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.18f)

    Column(modifier) {
        Text("Độ cao", style = MaterialTheme.typography.titleSmall)
        Text(
            "${minAlt.roundToInt()}–${maxAlt.roundToInt()} m",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 8.dp),
        ) {
            val w = size.width
            val h = size.height
            val totalDist = distances.last().coerceAtLeast(1.0)
            val altRange = (maxAlt - minAlt).coerceAtLeast(1.0)

            fun px(distance: Double) = (distance / totalDist * w).toFloat()
            fun py(alt: Double) = (h - (alt - minAlt) / altRange * h).toFloat()

            val area = Path().apply {
                moveTo(0f, h)
                distances.forEachIndexed { i, d -> lineTo(px(d), py(altitudes[i])) }
                lineTo(w, h)
                close()
            }
            drawPath(area, fillColor)

            var previous = Offset(px(distances.first()), py(altitudes.first()))
            for (i in 1 until distances.size) {
                val next = Offset(px(distances[i]), py(altitudes[i]))
                drawLine(lineColor, previous, next, strokeWidth = 2.dp.toPx())
                previous = next
            }
        }
    }
}
