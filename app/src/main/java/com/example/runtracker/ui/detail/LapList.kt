package com.example.runtracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.ActivityLap

/**
 * Bảng lap: mỗi km một dòng, thanh ngang dài theo tốc độ (km nhanh nhất = thanh dài nhất),
 * nhãn pace trực tiếp trên từng dòng. Một chuỗi dữ liệu, một màu.
 */
@Composable
fun LapList(
    laps: List<ActivityLap>,
    modifier: Modifier = Modifier,
) {
    if (laps.isEmpty()) return

    val speeds = laps.map { if (it.avgPaceSecPerKm > 0.0) 1_000.0 / it.avgPaceSecPerKm else 0.0 }
    val maxSpeed = speeds.max().coerceAtLeast(0.0001)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Lap (mỗi km)", style = MaterialTheme.typography.titleSmall)
        laps.forEachIndexed { i, lap ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    lapLabel(lap),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(56.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(18.dp)
                        .padding(end = 8.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction = (speeds[i] / maxSpeed).toFloat().coerceIn(0.02f, 1f))
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    formatPace(lap.avgPaceSecPerKm),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(88.dp),
                )
                Text(
                    formatClock(lap.duration.inWholeSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp),
                )
            }
        }
    }
}

private fun lapLabel(lap: ActivityLap): String =
    if (lap.distanceMeters >= 995.0) "Km ${lap.lapIndex}"
    else "${(lap.distanceMeters / 1000.0).let { "%.2f".format(it) }} km"
