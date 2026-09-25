package com.example.runtracker.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.runtracker.domain.training.PeriodBucket
import com.example.runtracker.ui.theme.Spacing

private val BAR_AREA_HEIGHT = 120.dp
private val BAR_WIDTH = 20.dp

/** Bar chart quãng đường theo kỳ (tuần/tháng/năm) — cột cao theo tỉ lệ so với kỳ lớn nhất trong danh sách. */
@Composable
fun ProgressBarChart(buckets: List<PeriodBucket>, modifier: Modifier = Modifier) {
    val maxDistance = buckets.maxOfOrNull { it.distanceMeters }?.coerceAtLeast(1.0) ?: 1.0
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        buckets.forEach { bucket ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.height(BAR_AREA_HEIGHT).width(BAR_WIDTH),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((BAR_AREA_HEIGHT.value * (bucket.distanceMeters / maxDistance).toFloat().coerceIn(0.03f, 1f)).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
