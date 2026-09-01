package com.example.runtracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.training.ZoneTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val heartRateAvailable by viewModel.heartRateAvailable.collectAsState()
    val zoneDistribution by viewModel.zoneDistribution.collectAsState()
    val segmentEfforts by viewModel.segmentEfforts.collectAsState()

    val heartRatePermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.containsAll(viewModel.heartRatePermissions)) {
            viewModel.onHeartRatePermissionGranted()
        }
    }

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

                is ActivityDetailUiState.Loaded -> LoadedContent(
                    state = s,
                    heartRateAvailable = heartRateAvailable,
                    importMessage = viewModel.importMessage,
                    zoneDistribution = zoneDistribution,
                    segmentEfforts = segmentEfforts,
                    onSyncHeartRate = {
                        viewModel.importHeartRateOrRequest {
                            heartRatePermissionLauncher.launch(viewModel.heartRatePermissions)
                        }
                    },
                    onSetRpe = viewModel::setPerceivedExertion,
                    onCreateSegment = viewModel::createSegment,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: ActivityDetailUiState.Loaded,
    heartRateAvailable: Boolean,
    importMessage: String?,
    zoneDistribution: List<ZoneTime>,
    segmentEfforts: List<SegmentEffortRow>,
    onSyncHeartRate: () -> Unit,
    onSetRpe: (Int) -> Unit,
    onCreateSegment: (String) -> Unit,
) {
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
            StatsPanel(state = state)
            if (state.canEnterRpe) {
                NoHeartRateSection(
                    rpe = state.activity.perceivedExertion,
                    heartRateAvailable = heartRateAvailable,
                    importMessage = importMessage,
                    onSyncHeartRate = onSyncHeartRate,
                    onSetRpe = onSetRpe,
                )
            }
            ElevationChart(points = state.routePoints, modifier = Modifier.fillMaxWidth())
            ZoneDistributionList(distribution = zoneDistribution, modifier = Modifier.fillMaxWidth())
            LapList(laps = state.laps, modifier = Modifier.fillMaxWidth())
            if (state.routePoints.size >= 2) {
                SegmentSection(efforts = segmentEfforts, onCreateSegment = onCreateSegment)
            }
        }
    }
}

@Composable
private fun StatsPanel(state: ActivityDetailUiState.Loaded, modifier: Modifier = Modifier) {
    val activity = state.activity
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
        Stat("Relative Effort (TRIMP)", state.trimp?.roundToInt()?.toString() ?: "—")
        activity.avgHeartRate?.let { Stat("Nhịp tim TB", "$it bpm") }
        activity.maxHeartRate?.let { Stat("Nhịp tim tối đa", "$it bpm") }
        activity.calories?.let { Stat("Calo", "$it kcal") }
        Stat("Điểm GPS", state.routePoints.size.toString())
    }
}

@Composable
private fun NoHeartRateSection(
    rpe: Int?,
    heartRateAvailable: Boolean,
    importMessage: String?,
    onSyncHeartRate: () -> Unit,
    onSetRpe: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chưa có nhịp tim", style = MaterialTheme.typography.titleSmall)

        if (heartRateAvailable) {
            Text(
                "Đồng bộ nhịp tim từ Health Connect (đồng hồ / vòng đeo), hoặc nhập RPE.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onSyncHeartRate) { Text("Đồng bộ nhịp tim") }
            importMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        } else {
            Text(
                "Health Connect không khả dụng — nhập RPE (1 rất nhẹ … 10 kiệt sức) để tính TRIMP.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RpeEditor(current = rpe, onSave = onSetRpe)
    }
}

@Composable
private fun RpeEditor(current: Int?, onSave: (Int) -> Unit) {
    var rpe by remember(current) { mutableIntStateOf(current ?: 5) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Cảm giác gắng sức (RPE): $rpe", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = rpe.toFloat(),
            onValueChange = { rpe = it.roundToInt() },
            valueRange = 1f..10f,
            steps = 8,
        )
        Button(onClick = { onSave(rpe) }) {
            Text(if (current == null) "Lưu RPE" else "Cập nhật RPE")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun SegmentSection(
    efforts: List<SegmentEffortRow>,
    onCreateSegment: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Segments", style = MaterialTheme.typography.titleSmall)
        efforts.forEach { effort ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    effort.segmentName,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    formatClock(effort.elapsedSeconds.toLong()),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                )
            }
        }
        OutlinedButton(onClick = { showDialog = true }) { Text("Tạo segment từ buổi này") }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tên segment") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Ví dụ: Dốc cầu Sài Gòn") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onCreateSegment(name.trim())
                        showDialog = false
                    },
                ) { Text("Tạo") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Huỷ") }
            },
        )
    }
}
