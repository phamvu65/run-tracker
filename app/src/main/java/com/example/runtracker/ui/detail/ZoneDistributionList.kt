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
import com.example.runtracker.domain.training.ZoneTime

/**
 * Thời gian trong từng vùng nhịp tim — thanh ngang theo tỉ lệ, một hue tăng dần cường độ.
 */
@Composable
fun ZoneDistributionList(
    distribution: List<ZoneTime>,
    modifier: Modifier = Modifier,
) {
    val total = distribution.sumOf { it.seconds }
    if (total <= 0L) return

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Thời gian theo vùng nhịp tim", style = MaterialTheme.typography.titleSmall)
        distribution.sortedBy { it.zoneIndex }.forEach { zone ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Z${zone.zoneIndex}",
                    Modifier.width(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box(Modifier.weight(1f).height(16.dp).padding(end = 8.dp)) {
                    if (zone.seconds > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth((zone.seconds.toFloat() / total).coerceIn(0.02f, 1f))
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(zoneColor(zone.zoneIndex)),
                        )
                    }
                }
                Text(
                    formatClock(zone.seconds),
                    Modifier.width(64.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun zoneColor(index: Int) =
    MaterialTheme.colorScheme.primary.copy(alpha = (0.30f + 0.15f * (index - 1)).coerceAtMost(1f))
