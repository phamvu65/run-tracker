package com.example.runtracker.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.ui.theme.formatDistanceUnit
import com.example.runtracker.ui.theme.formatPaceUnit
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.FeedActivityCard
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FEED_TIME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'thg' M, yyyy 'lúc' HH:mm", Locale.forLanguageTag("vi"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListScreen(
    onActivityClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    onAddManual: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ActivityListViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsState()
    val athleteName by viewModel.athleteName.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            com.example.runtracker.ui.components.CompactTopHeader(
                title = "Hoạt động",
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                } else null,
                actions = {
                    IconButton(onClick = onAddManual) {
                        Icon(Icons.Filled.Add, contentDescription = "Nhập buổi tập thủ công")
                    }
                },
            )
        },
    ) { padding ->
        if (feed.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Chưa có buổi tập nào",
                    message = "Sang tab Ghi và nhấn nút GHI để bắt đầu buổi chạy đầu tiên.",
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(feed, key = { it.activity.id }) { item ->
                    val a = item.activity
                    FeedActivityCard(
                        athleteName = athleteName,
                        timeText = a.startTime.atZone(ZoneId.systemDefault()).format(FEED_TIME),
                        title = activityTitle(a),
                        stats = listOf(
                            StatCell("Quãng đường", formatDistanceUnit(a.distanceMeters)),
                            StatCell("Nhịp độ", formatPaceUnit(a.avgPaceSecPerKm)),
                            StatCell("Thời gian", formatClock(a.movingTime.inWholeSeconds)),
                        ),
                        locationText = a.locationName,
                        achievement = item.achievement,
                        routePoints = item.routePoints,
                        onClick = { onActivityClick(a.id) },
                    )
                }
            }
        }
    }
}

private fun activityTitle(activity: Activity): String {
    val hour = activity.startTime.atZone(ZoneId.systemDefault()).hour
    val verb = when (activity.type) {
        com.example.runtracker.domain.model.ActivityType.CYCLING -> "Đạp xe"
        com.example.runtracker.domain.model.ActivityType.WALKING -> "Đi bộ"
        else -> "Chạy bộ"
    }
    val part = when (hour) {
        in 5..10 -> "buổi sáng"
        in 11..13 -> "buổi trưa"
        in 14..17 -> "buổi chiều"
        in 18..21 -> "buổi tối"
        else -> "buổi đêm"
    }
    return "$verb $part"
}
