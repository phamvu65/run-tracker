package com.example.runtracker.ui.detail

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.FeatureFlags
import com.example.runtracker.core.formatClock
import com.example.runtracker.domain.model.ActivityWeather
import com.example.runtracker.domain.tracking.RunAggregator
import com.example.runtracker.domain.training.ZoneTime
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.LabeledValue
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.components.StatStrip
import com.example.runtracker.ui.theme.Spacing
import com.example.runtracker.ui.theme.formatDistanceUnit
import com.example.runtracker.ui.theme.formatPaceUnit
import com.example.runtracker.ui.theme.formatSpeedUnit
import kotlinx.coroutines.launch
import java.io.File
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

    val heartRatePermissionLauncher = if (FeatureFlags.HEART_RATE_INTEGRATION) rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.containsAll(viewModel.heartRatePermissions)) {
            viewModel.onHeartRatePermissionGranted()
        }
    } else null

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
                    onBack = onBack,
                    onSyncHeartRate = {
                        viewModel.importHeartRateOrRequest {
                            heartRatePermissionLauncher?.launch(viewModel.heartRatePermissions)
                        }
                    },
                    onSetRpe = viewModel::setPerceivedExertion,
                    onRefreshWeather = viewModel::refreshWeather,
                    onCreateSegment = onCreateSegment,
                    onExportGpx = {
                        coroutineScope.launch {
                            viewModel.exportGpx()?.let { gpx -> shareGpx(context, s.activity.id, gpx) }
                        }
                    },
                )
            }
        }
    }
}

/** Chiều cao bản đồ đầu màn — lớn, tràn viền kiểu Strava thay vì một dải nhỏ 240dp trước đây. */
private val MAP_HEIGHT = 420.dp

@Composable
private fun LoadedContent(
    state: ActivityDetailUiState.Loaded,
    heartRateAvailable: Boolean,
    importMessage: String?,
    weatherMessage: String?,
    zoneDistribution: List<ZoneTime>,
    segmentEfforts: List<SegmentEffortRow>,
    onBack: () -> Unit,
    onSyncHeartRate: () -> Unit,
    onSetRpe: (Int) -> Unit,
    onRefreshWeather: () -> Unit,
    onCreateSegment: () -> Unit,
    onExportGpx: () -> Unit,
) {
    val activity = state.activity
    val traceElevation = remember(state.routePoints) {
        state.routePoints.takeIf { points ->
            points.size >= 2 && points.any { it.altitude.isFinite() && it.altitude != 0.0 }
        }?.let(RunAggregator::fromPoints)
    }
    val elevationGain = traceElevation?.elevationGainMeters ?: activity.elevationGainMeters
    val elevationLoss = traceElevation?.elevationLossMeters ?: activity.elevationLossMeters
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(MAP_HEIGHT)) {
            if (state.routePoints.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Không có dữ liệu GPS", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                RoutePlaybackMap(points = state.routePoints, modifier = Modifier.fillMaxSize())
            }

            // Nút back nổi trực tiếp trên bản đồ thay cho TopAppBar đặc — Scaffold đã chừa an
            // toàn status bar nên không cần statusBarsPadding() thêm ở đây (tránh đệm 2 lần).
            FloatingBackButton(onBack, modifier = Modifier.align(Alignment.TopStart).padding(Spacing.md))
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
                        StatCell("Quãng đường", formatDistanceUnit(activity.distanceMeters)),
                        StatCell("Pace", formatPaceUnit(activity.avgPaceSecPerKm)),
                        StatCell("Thời gian di chuyển", formatClock(activity.movingTime.inWholeSeconds)),
                    ),
                )
                androidx.compose.material3.HorizontalDivider(
                    Modifier.padding(vertical = Spacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                LabeledValue("Thời gian tổng", formatClock(activity.duration.inWholeSeconds))
                LabeledValue("Tốc độ TB", formatSpeedUnit(activity.avgSpeedKmh))
                LabeledValue(
                    "Độ cao lên / xuống",
                    "${elevationGain.roundToInt()} / ${elevationLoss.roundToInt()} m",
                )
                LabeledValue("Relative Effort (TRIMP)", state.trimp?.roundToInt()?.toString() ?: "—")
                if (FeatureFlags.HEART_RATE_INTEGRATION) {
                    activity.avgHeartRate?.let { LabeledValue("Nhịp tim TB", "$it bpm") }
                    activity.maxHeartRate?.let { LabeledValue("Nhịp tim tối đa", "$it bpm") }
                }
                activity.calories?.let { LabeledValue("Calo", "$it kcal") }
                activity.steps?.let { LabeledValue("Số bước", "$it") }
                activity.avgCadence?.let { LabeledValue("Cadence TB", "$it spm") }
                LabeledValue("Điểm GPS", state.routePoints.size.toString())
            }

            if (state.routePoints.isNotEmpty()) {
                OutlinedButton(onClick = onExportGpx) { Text("Xuất GPX") }
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

            if (FeatureFlags.HEART_RATE_INTEGRATION && zoneDistribution.isNotEmpty()) {
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
    SectionHeader("Mức độ gắng sức")
    Spacer(Modifier.height(Spacing.sm))
    if (FeatureFlags.HEART_RATE_INTEGRATION && heartRateAvailable) {
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
            "Đánh giá cảm giác sau buổi tập từ 1 (rất nhẹ) đến 10 (kiệt sức) để tính tải tập luyện.",
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

/** Ghi file GPX ra cache rồi mở bảng chia sẻ chuẩn Android — cần `content://` qua FileProvider,
 * `file://` trực tiếp bị chặn (FileUriExposedException) từ Android 7+. */
private fun shareGpx(context: android.content.Context, activityId: String, gpxContent: String) {
    val dir = File(context.cacheDir, "gpx").apply { mkdirs() }
    val file = File(dir, "activity_$activityId.gpx")
    file.writeText(gpxContent)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gpx+xml"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Xuất GPX"))
}

/** Nút back tròn nổi trên bản đồ — cùng kiểu "Map Guide" (nền tối trong suốt + viền mảnh) với
 * cụm nút đổi kiểu bản đồ/về vị trí của [com.example.runtracker.ui.common.OsmMap], thay cho
 * TopAppBar đặc để bản đồ được nhìn trọn vẹn, không bị thanh tiêu đề ăn bớt chiều cao. */
@Composable
private fun FloatingBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 2.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
        }
    }
}
