package com.example.runtracker.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.runtracker.domain.model.ActivityWeather
import com.example.runtracker.domain.training.ZoneTime
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.LabeledValue
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.components.StatStrip
import com.example.runtracker.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val HEADER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy · HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onBack: () -> Unit,
    onCreateSegment: () -> Unit = {},
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết buổi tập") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
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
                    weatherMessage = viewModel.weatherMessage,
                    zoneDistribution = zoneDistribution,
                    segmentEfforts = segmentEfforts,
                    onSyncHeartRate = {
                        viewModel.importHeartRateOrRequest {
                            heartRatePermissionLauncher.launch(viewModel.heartRatePermissions)
                        }
                    },
                    onSetRpe = viewModel::setPerceivedExertion,
                    onRefreshWeather = viewModel::refreshWeather,
                    onCreateSegment = onCreateSegment,
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
    weatherMessage: String?,
    zoneDistribution: List<ZoneTime>,
    segmentEfforts: List<SegmentEffortRow>,
    onSyncHeartRate: () -> Unit,
    onSetRpe: (Int) -> Unit,
    onRefreshWeather: () -> Unit,
    onCreateSegment: () -> Unit,
) {
    val activity = state.activity
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (state.routePoints.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Không có dữ liệu GPS", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            RouteMap(
                points = state.routePoints,
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                "${activity.type} · ${activity.startTime.atZone(ZoneId.systemDefault()).format(HEADER_DATE)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlatCard {
                StatStrip(
                    listOf(
                        StatCell("Quãng đường", formatDistanceKm(activity.distanceMeters)),
                        StatCell("Pace", formatPace(activity.avgPaceSecPerKm)),
                        StatCell("Thời gian di chuyển", formatClock(activity.movingTime.inWholeSeconds)),
                    ),
                )
                androidx.compose.material3.HorizontalDivider(
                    Modifier.padding(vertical = Spacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                LabeledValue("Thời gian tổng", formatClock(activity.duration.inWholeSeconds))
                LabeledValue("Tốc độ TB", "%.1f km/h".format(activity.avgSpeedKmh))
                LabeledValue(
                    "Độ cao lên / xuống",
                    "${activity.elevationGainMeters.roundToInt()} / ${activity.elevationLossMeters.roundToInt()} m",
                )
                LabeledValue("Relative Effort (TRIMP)", state.trimp?.roundToInt()?.toString() ?: "—")
                activity.avgHeartRate?.let { LabeledValue("Nhịp tim TB", "$it bpm") }
                activity.maxHeartRate?.let { LabeledValue("Nhịp tim tối đa", "$it bpm") }
                activity.calories?.let { LabeledValue("Calo", "$it kcal") }
                LabeledValue("Điểm GPS", state.routePoints.size.toString())
            }

            if (state.canEnterRpe) {
                FlatCard {
                    NoHeartRateSection(
                        rpe = activity.perceivedExertion,
                        heartRateAvailable = heartRateAvailable,
                        importMessage = importMessage,
                        onSyncHeartRate = onSyncHeartRate,
                        onSetRpe = onSetRpe,
                    )
                }
            }

            FlatCard {
                WeatherSection(
                    weather = activity.weather,
                    message = weatherMessage,
                    hasGps = state.routePoints.isNotEmpty(),
                    onRefreshWeather = onRefreshWeather,
                )
            }

            FlatCard {
                SectionHeader("Độ cao")
                Spacer(Modifier.height(Spacing.sm))
                ElevationChart(points = state.routePoints, modifier = Modifier.fillMaxWidth())
            }

            if (zoneDistribution.isNotEmpty()) {
                FlatCard {
                    SectionHeader("Thời gian theo vùng nhịp tim")
                    Spacer(Modifier.height(Spacing.sm))
                    ZoneDistributionList(distribution = zoneDistribution, modifier = Modifier.fillMaxWidth())
                }
            }

            if (state.laps.isNotEmpty()) {
                FlatCard {
                    SectionHeader("Chặng (mỗi km)")
                    Spacer(Modifier.height(Spacing.sm))
                    LapList(laps = state.laps, modifier = Modifier.fillMaxWidth())
                }
            }

            if (state.routePoints.size >= 2) {
                FlatCard {
                    SegmentSection(efforts = segmentEfforts, onCreateSegment = onCreateSegment)
                }
            }
        }
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
    SectionHeader("Chưa có nhịp tim")
    Spacer(Modifier.height(Spacing.sm))
    if (heartRateAvailable) {
        Text(
            "Đồng bộ nhịp tim từ Health Connect (đồng hồ / vòng đeo), hoặc nhập RPE.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedButton(onClick = onSyncHeartRate) { Text("Đồng bộ nhịp tim") }
        importMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    } else {
        Text(
            "Health Connect không khả dụng — nhập RPE (1 rất nhẹ … 10 kiệt sức) để tính TRIMP.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    RpeEditor(current = rpe, onSave = onSetRpe)
}

@Composable
private fun RpeEditor(current: Int?, onSave: (Int) -> Unit) {
    var rpe by remember(current) { mutableIntStateOf(current ?: 5) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
private fun WeatherSection(
    weather: ActivityWeather?,
    message: String?,
    hasGps: Boolean,
    onRefreshWeather: () -> Unit,
) {
    SectionHeader("Thời tiết")
    Spacer(Modifier.height(Spacing.xs))
    if (weather != null) {
        LabeledValue("Điều kiện", weatherCodeDescription(weather.weatherCode))
        LabeledValue("Nhiệt độ", "%.0f°C".format(weather.temperatureC))
        weather.apparentTemperatureC?.let { LabeledValue("Cảm giác như", "%.0f°C".format(it)) }
        weather.humidityPct?.let { LabeledValue("Độ ẩm", "$it%") }
        weather.windSpeedMps?.let { mps ->
            val dir = windCompass(weather.windDirectionDeg)
            LabeledValue("Gió", "%.0f km/h".format(mps * 3.6) + if (dir.isNotEmpty()) " $dir" else "")
        }
        Spacer(Modifier.height(Spacing.xs))
        OutlinedButton(onClick = onRefreshWeather) { Text("Cập nhật lại") }
    } else {
        Text(
            if (hasGps) {
                "Chưa có dữ liệu thời tiết cho buổi tập này."
            } else {
                "Buổi tập không có GPS nên không tra được thời tiết."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasGps) {
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(onClick = onRefreshWeather) { Text("Lấy thời tiết") }
        }
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Spacing.xs)) }
}

@Composable
private fun SegmentSection(
    efforts: List<SegmentEffortRow>,
    onCreateSegment: () -> Unit,
) {
    SectionHeader("Segments")
    Spacer(Modifier.height(Spacing.sm))
    efforts.forEach { effort ->
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
            Text(
                effort.segmentName,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                formatClock(effort.elapsedSeconds.toLong()),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.End,
            )
        }
    }
    Spacer(Modifier.height(Spacing.xs))
    OutlinedButton(onClick = onCreateSegment) { Text("Tạo segment từ buổi này") }
}
