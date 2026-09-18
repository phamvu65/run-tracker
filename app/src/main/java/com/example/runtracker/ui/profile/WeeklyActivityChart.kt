package com.example.runtracker.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.ui.theme.Spacing
import kotlin.math.roundToInt

private val DAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
private val TOOLTIP_WIDTH = 168.dp

/**
 * Biểu đồ 7 ngày (Thứ Hai → Chủ Nhật) kiểu GoRun "Diagram Card Weekly": đường + vùng tô theo
 * quãng đường mỗi ngày. Chạm hoặc kéo ngang để chọn 1 ngày — bong bóng số liệu nổi phía trên
 * hiện quãng đường / pace / thời gian của ngày đó (trống nếu ngày đó không tập).
 */
@Composable
fun WeeklyActivityChart(
    days: List<DayStat>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (days.size != 7) return
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val selected = days[selectedIndex]
    val density = LocalDensity.current
    var canvasWidthPx by remember { mutableStateOf(0f) }

    Column(modifier) {
        Box {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                    .pointerInput(days) {
                        val stepX = size.width / 6f
                        detectTapGestures(
                            onPress = { offset -> onSelect((offset.x / stepX).roundToInt().coerceIn(0, 6)) },
                        )
                    }
                    .pointerInput(days) {
                        val stepX = size.width / 6f
                        detectHorizontalDragGestures { change, _ ->
                            onSelect((change.position.x / stepX).roundToInt().coerceIn(0, 6))
                            change.consume()
                        }
                    },
            ) {
                val maxKm = (days.maxOf { it.distanceMeters } / 1000.0).coerceAtLeast(1.0)
                val stepX = size.width / 6f
                val plotHeight = size.height * 0.82f
                fun x(i: Int) = i * stepX
                fun y(km: Double) = (size.height - (km / maxKm) * plotHeight).toFloat()

                listOf(0.5f, 1f).forEach { f ->
                    val gy = size.height - f * plotHeight
                    drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                }

                val linePath = Path()
                val fillPath = Path().apply { moveTo(0f, size.height) }
                days.forEachIndexed { i, d ->
                    val px = x(i)
                    val py = y(d.distanceMeters / 1000.0)
                    if (i == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
                    fillPath.lineTo(px, py)
                }
                fillPath.lineTo(x(6), size.height)
                fillPath.close()
                drawPath(fillPath, fill)
                drawPath(linePath, line, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                days.forEachIndexed { i, d ->
                    val isSelected = i == selectedIndex
                    drawCircle(
                        line,
                        radius = if (isSelected) 7f else 4f,
                        center = Offset(x(i), y(d.distanceMeters / 1000.0)),
                    )
                }
            }

            if (selected.hasActivity && canvasWidthPx > 0f) {
                val tooltipWidthPx = with(density) { TOOLTIP_WIDTH.toPx() }
                val stepPx = canvasWidthPx / 6f
                val xPx = (selectedIndex * stepPx - tooltipWidthPx / 2f)
                    .coerceIn(0f, (canvasWidthPx - tooltipWidthPx).coerceAtLeast(0f))
                val xDp = with(density) { xPx.toDp() }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .padding(start = xDp, top = 8.dp)
                        .width(TOOLTIP_WIDTH),
                ) {
                    Column(
                        Modifier.padding(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val paceSecPerKm = if (selected.distanceMeters > 0) {
                            selected.movingTime.inWholeSeconds / (selected.distanceMeters / 1000.0)
                        } else {
                            0.0
                        }
                        TooltipRow(Icons.Filled.LocationOn, formatDistanceKm(selected.distanceMeters))
                        TooltipRow(Icons.Filled.DirectionsRun, formatPace(paceSecPerKm))
                        TooltipRow(Icons.Filled.Timer, formatClock(selected.movingTime.inWholeSeconds))
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DAY_LABELS.forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selectedIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun TooltipRow(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
