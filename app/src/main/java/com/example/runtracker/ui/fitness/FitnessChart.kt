package com.example.runtracker.ui.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot

/**
 * CTL (Fitness) và ATL (Fatigue) theo thời gian — hai chuỗi cùng đơn vị, một trục y,
 * có legend + nhãn giá trị cuối. TSB hiển thị riêng ở các ô số phía trên.
 */
@Composable
fun FitnessChart(
    snapshots: List<FitnessFreshnessSnapshot>,
    modifier: Modifier = Modifier,
) {
    if (snapshots.isEmpty()) return

    val ctlColor = MaterialTheme.colorScheme.primary
    val atlColor = MaterialTheme.colorScheme.tertiary

    val ctl = remember(snapshots) { snapshots.map { it.ctl } }
    val atl = remember(snapshots) { snapshots.map { it.atl } }
    val maxValue = remember(snapshots) { (ctl + atl).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0 }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = ctlColor)

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(ctlColor, "Fitness (CTL)")
            LegendItem(atlColor, "Fatigue (ATL)")
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 8.dp),
        ) {
            val inset = 6.dp.toPx()
            val w = (size.width - 2 * inset).coerceAtLeast(0f)
            val h = (size.height - 2 * inset).coerceAtLeast(0f)
            fun px(i: Int) = inset + if (ctl.size == 1) w / 2 else i.toFloat() / (ctl.size - 1) * w
            fun py(v: Double) = inset + (h - (v / maxValue) * h).toFloat()

            // Vùng tô dưới đường CTL, làm nổi bật xu hướng Fitness — cùng kỹ thuật ElevationChart.
            val ctlPath = Path().apply {
                moveTo(px(0), h)
                ctl.forEachIndexed { i, v -> lineTo(px(i), py(v)) }
                lineTo(px(ctl.lastIndex), h)
                close()
            }
            drawPath(ctlPath, color = ctlColor.copy(alpha = 0.16f))

            fun line(values: List<Double>, color: Color) {
                var prev = Offset(px(0), py(values[0]))
                for (i in 1 until values.size) {
                    val next = Offset(px(i), py(values[i]))
                    drawLine(color, prev, next, strokeWidth = 2.dp.toPx())
                    prev = next
                }
            }
            line(ctl, ctlColor)
            line(atl, atlColor)

            // Chấm + nhãn giá trị mới nhất cuối mỗi đường.
            val lastCtl = Offset(px(ctl.lastIndex), py(ctl.last()))
            val lastAtl = Offset(px(atl.lastIndex), py(atl.last()))
            drawCircle(ctlColor, radius = 4.dp.toPx(), center = lastCtl)
            drawCircle(atlColor, radius = 4.dp.toPx(), center = lastAtl)
            val ctlText = textMeasurer.measure(ctl.last().toInt().toString(), style = labelStyle)
            drawText(
                textMeasurer,
                ctl.last().toInt().toString(),
                topLeft = Offset(
                    (lastCtl.x - ctlText.size.width).coerceAtLeast(0f),
                    (lastCtl.y - ctlText.size.height - 4.dp.toPx()).coerceAtLeast(0f),
                ),
                style = labelStyle,
            )
        }
        if (snapshots.size == 1) {
            Text("Đã có dữ liệu 1 ngày; biểu đồ xu hướng sẽ nối các điểm từ ngày tiếp theo.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
