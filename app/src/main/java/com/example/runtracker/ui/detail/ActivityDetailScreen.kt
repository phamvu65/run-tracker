package com.example.runtracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.Activity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết buổi tập") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                ActivityDetailUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                ActivityDetailUiState.NotFound ->
                    Text("Không tìm thấy buổi tập", modifier = Modifier.align(Alignment.Center))

                is ActivityDetailUiState.Loaded -> LoadedContent(s)
            }
        }
    }
}

@Composable
private fun LoadedContent(state: ActivityDetailUiState.Loaded) {
    Column(Modifier.fillMaxSize()) {
        if (state.routePoints.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Không có dữ liệu GPS", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            RouteMap(
                points = state.routePoints,
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatsPanel(activity = state.activity, pointCount = state.routePoints.size)
            ElevationChart(points = state.routePoints, modifier = Modifier.fillMaxWidth())
            LapList(laps = state.laps, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatsPanel(activity: Activity, pointCount: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${activity.type} · ${activity.startTime}",
            style = MaterialTheme.typography.titleSmall,
        )
        Stat("Quãng đường", formatDistanceKm(activity.distanceMeters))
        Stat("Thời gian", formatClock(activity.duration.inWholeSeconds))
        Stat("Thời gian di chuyển", formatClock(activity.movingTime.inWholeSeconds))
        Stat("Pace", formatPace(activity.avgPaceSecPerKm))
        Stat("Tốc độ TB", "%.1f km/h".format(activity.avgSpeedKmh))
        Stat(
            "Độ cao +/-",
            "${activity.elevationGainMeters.roundToInt()} / ${activity.elevationLossMeters.roundToInt()} m",
        )
        activity.avgHeartRate?.let { Stat("Nhịp tim TB", "$it bpm") }
        activity.maxHeartRate?.let { Stat("Nhịp tim tối đa", "$it bpm") }
        activity.calories?.let { Stat("Calo", "$it kcal") }
        Stat("Điểm GPS", pointCount.toString())
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}
