package com.example.runtracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.ui.components.IconStatChip
import com.example.runtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

/**
 * Lưới ngày kiểu GoRun "Diagram Card Monthly": mỗi ô là một ngày trong [month], ngày có buổi
 * tập được tô nền; chạm vào một ngày để xem số liệu ngày đó ở hàng ngay dưới lưới.
 */
@Composable
fun MonthlyCalendarGrid(
    month: YearMonth,
    days: Map<LocalDate, DayStat>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1 // Thứ Hai = 0
    val totalCells = firstDayOffset + month.lengthOfMonth()
    val rowCount = (totalCells + 6) / 7

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        repeat(rowCount) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { col ->
                    val dayNum = row * 7 + col - firstDayOffset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            DayCell(
                                day = dayNum,
                                hasActivity = days[date]?.hasActivity == true,
                                isSelected = date == selectedDate,
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }

        val selectedStat = days[selectedDate]
        if (selectedStat != null && selectedStat.hasActivity) {
            val paceSecPerKm = selectedStat.movingTime.inWholeSeconds / (selectedStat.distanceMeters / 1000.0)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                IconStatChip(Icons.Filled.LocationOn, formatDistanceKm(selectedStat.distanceMeters))
                IconStatChip(Icons.Filled.DirectionsRun, formatPace(paceSecPerKm))
                IconStatChip(Icons.Filled.Timer, formatClock(selectedStat.movingTime.inWholeSeconds))
            }
        } else {
            Text(
                "Không có buổi tập ngày này",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCell(day: Int, hasActivity: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .then(
                when {
                    hasActivity -> Modifier.background(MaterialTheme.colorScheme.primary)
                    else -> Modifier
                },
            )
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (hasActivity) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
