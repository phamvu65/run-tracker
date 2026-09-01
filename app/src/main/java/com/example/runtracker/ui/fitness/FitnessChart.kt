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
    if (snapshots.size < 2) return

    val ctlColor = MaterialTheme.colorScheme.primary
    val atlColor = MaterialTheme.colorScheme.tertiary

    val ctl = remember(snapshots) { snapshots.map { it.ctl } }
    val atl = remember(snapshots) { snapshots.map { it.atl } }
    val maxValue = remember(snapshots) { (ctl + atl).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0 }

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
            val w = size.width
            val h = size.height
            fun px(i: Int) = if (ctl.size == 1) 0f else i.toFloat() / (ctl.size - 1) * w
            fun py(v: Double) = (h - (v / maxValue) * h).toFloat()

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
