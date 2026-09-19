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
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.BestEffort
import com.example.runtracker.ui.components.AppListCard
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HISTORY_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'thg' M, yyyy", Locale.forLanguageTag("vi"))

private fun medalEmoji(rank: Int) = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    else -> "🥉"
}

private fun BestEffort.paceText() = formatPace(elapsedSeconds / (distance.meters / 1000.0))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsScreen(
    onBack: () -> Unit,
    onActivityClick: (String) -> Unit,
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
                Column {
                    SectionHeader("Kỷ lục cá nhân")
                    Spacer(Modifier.height(Spacing.xs))
                }
            }
            items(currentRecords, key = { "record_${it.distance.name}" }) { record ->
                AppListCard(
                    title = record.distance.label,
                    subtitle = record.paceText(),
                    onClick = { onActivityClick(record.activityId) },
                    trailing = {
                        Text(formatClock(record.elapsedSeconds), style = MaterialTheme.typography.titleMedium)
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
                items(history, key = { "${it.activityId}_${it.distance.name}" }) { entry ->
                    AppListCard(
                        title = "${medalEmoji(entry.rank)}  ${entry.distance.label} nhanh thứ ${entry.rank}",
                        subtitle = entry.achievedAt.atZone(ZoneId.systemDefault()).format(HISTORY_DATE),
                        onClick = { onActivityClick(entry.activityId) },
                        trailing = {
                            Text(formatClock(entry.elapsedSeconds), style = MaterialTheme.typography.titleSmall)
                        },
                    )
                }
            }
        }
    }
}
