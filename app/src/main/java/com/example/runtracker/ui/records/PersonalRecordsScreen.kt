package com.example.runtracker.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.domain.training.ActivityRecordType
import com.example.runtracker.ui.components.AppListCard
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing
import com.example.runtracker.ui.theme.formatDistanceUnit
import com.example.runtracker.ui.theme.formatPaceUnit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val HISTORY_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'thg' M, yyyy", Locale.forLanguageTag("vi"))

private fun medalEmoji(rank: Int) = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    else -> "🥉"
}

private fun RecordEntry.title(): String = when (this) {
    is RecordEntry.Distance -> effort.distance.label
    is RecordEntry.Whole -> record.type.label
}

/** Giá trị chính hiển thị bên phải thẻ — thời gian (cự ly chuẩn) hoặc pace/km/mét (toàn buổi). */
@Composable
private fun RecordEntry.primaryValueText(): String = when (this) {
    is RecordEntry.Distance -> formatClock(effort.elapsedSeconds)
    is RecordEntry.Whole -> when (record.type) {
        ActivityRecordType.FASTEST_PACE -> formatPaceUnit(record.value)
        ActivityRecordType.LONGEST_DISTANCE -> formatDistanceUnit(record.value)
        ActivityRecordType.MOST_ELEVATION_GAIN -> "${record.value.roundToInt()} m"
    }
}

/** Subtitle phụ ở dòng kỷ lục hiện tại — chỉ cự ly chuẩn mới có pace tương ứng để hiện thêm. */
@Composable
private fun RecordEntry.subtitleText(): String? = when (this) {
    is RecordEntry.Distance -> formatPaceUnit(effort.elapsedSeconds / (effort.distance.meters / 1000.0))
    is RecordEntry.Whole -> null
}

private fun RecordEntry.historyTitle(): String = when (this) {
    is RecordEntry.Distance -> "${effort.distance.label} nhanh thứ ${effort.rank}"
    is RecordEntry.Whole -> "${record.type.label} thứ ${record.rank}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsScreen(
    onBack: () -> Unit,
    onActivityClick: (String) -> Unit,
    onOpenProgress: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PersonalRecordsViewModel = hiltViewModel(),
) {
    val currentRecords by viewModel.currentRecords.collectAsState()
    val history by viewModel.history.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Thành tích") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (currentRecords.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Chưa có kỷ lục nào",
                    message = "Chạy liên tục đủ 1km trở lên trong một buổi tập để bắt đầu ghi nhận kỷ lục cá nhân.",
                    action = {
                        AppListCard(title = "Xem tiến độ theo tuần/tháng/năm", onClick = onOpenProgress)
                    },
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                AppListCard(title = "Xem tiến độ theo tuần/tháng/năm", onClick = onOpenProgress)
                Spacer(Modifier.height(Spacing.sm))
            }
            item {
                Column {
                    SectionHeader("Kỷ lục cá nhân")
                    Spacer(Modifier.height(Spacing.xs))
                }
            }
            items(currentRecords, key = { "record_${it.title()}" }) { entry ->
                AppListCard(
                    title = entry.title(),
                    subtitle = entry.subtitleText(),
                    onClick = { onActivityClick(entry.activityId) },
                    trailing = {
                        Text(entry.primaryValueText(), style = MaterialTheme.typography.titleMedium)
                    },
                )
            }

            if (history.isNotEmpty()) {
                item {
                    Column {
                        Spacer(Modifier.height(Spacing.md))
                        SectionHeader("Lịch sử thành tích")
                        Spacer(Modifier.height(Spacing.xs))
                    }
                }
                items(history, key = { "${it.activityId}_${it.title()}_${it.achievedAt}" }) { entry ->
                    AppListCard(
                        title = "${medalEmoji(entry.rank)}  ${entry.historyTitle()}",
                        subtitle = entry.achievedAt.atZone(ZoneId.systemDefault()).format(HISTORY_DATE),
                        onClick = { onActivityClick(entry.activityId) },
                        trailing = {
                            Text(entry.primaryValueText(), style = MaterialTheme.typography.titleSmall)
                        },
                    )
                }
            }
        }
    }
}
