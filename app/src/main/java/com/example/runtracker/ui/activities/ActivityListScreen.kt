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
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.ui.components.ActivityCard
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CARD_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM · HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListScreen(
    onActivityClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ActivityListViewModel = hiltViewModel(),
) {
    val activities by viewModel.activities.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Hoạt động") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (activities.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Chưa có buổi tập nào",
                    message = "Sang tab Ghi và nhấn nút GHI để bắt đầu buổi chạy đầu tiên.",
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(activities, key = { it.id }) { activity ->
                    ActivityCard(
                        title = activityTitle(activity),
                        subtitle = activity.startTime.atZone(ZoneId.systemDefault()).format(CARD_DATE),
                        stats = listOf(
                            StatCell("Quãng đường", formatDistanceKm(activity.distanceMeters)),
                            StatCell("Pace", formatPace(activity.avgPaceSecPerKm)),
                            StatCell("Thời gian", formatClock(activity.movingTime.inWholeSeconds)),
                        ),
                        onClick = { onActivityClick(activity.id) },
                    )
                }
            }
        }
    }
}

private fun activityTitle(activity: Activity): String {
    val hour = activity.startTime.atZone(ZoneId.systemDefault()).hour
    val part = when (hour) {
        in 5..10 -> "Chạy buổi sáng"
        in 11..13 -> "Chạy buổi trưa"
        in 14..17 -> "Chạy buổi chiều"
        in 18..21 -> "Chạy buổi tối"
        else -> "Chạy đêm"
    }
    return "${activity.type} · $part"
}
