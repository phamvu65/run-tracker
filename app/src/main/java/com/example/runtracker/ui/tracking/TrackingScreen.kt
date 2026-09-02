package com.example.runtracker.ui.tracking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.BatteryOptimization
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.core.trackingPermissions
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Route
import com.example.runtracker.tracking.LocationTrackingService
import com.example.runtracker.tracking.TrackingState
import com.example.runtracker.tracking.TrackingStatus
import com.example.runtracker.ui.components.ActivityCard
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.components.StatStrip
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing
import com.google.android.gms.maps.model.LatLng
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val CARD_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM · HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    onActivityClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.tracking.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val interruptedId by viewModel.interruptedActivityId.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val liveTrace by viewModel.liveTrace.collectAsState()
    val beacon by viewModel.beacon.collectAsState()
    var showRoutePicker by remember { mutableStateOf(false) }
    var voiceEnabled by rememberSaveable { mutableStateOf(true) }

    val speak = rememberRouteVoice()
    var lastSpokenStep by remember { mutableIntStateOf(-1) }
    var lastOffRoute by remember { mutableStateOf(false) }
    LaunchedEffect(state.navStepIndex, state.navOffRoute, state.navRouteName, voiceEnabled) {
        if (!voiceEnabled || state.status == TrackingStatus.IDLE || state.navRouteName == null) {
            return@LaunchedEffect
        }
        if (state.navOffRoute) {
            if (!lastOffRoute) speak("Đã đi chệch tuyến đường")
        } else if (state.navStepIndex != lastSpokenStep) {
            state.navInstruction?.let { speak(it) }
            lastSpokenStep = state.navStepIndex
        }
        lastOffRoute = state.navOffRoute
    }

    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) pendingAction?.invoke()
        pendingAction = null
    }

    fun withPermission(action: () -> Unit) {
        if (context.hasLocationPermission()) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(trackingPermissions())
        }
    }

    if (showRoutePicker) {
        RoutePickerDialog(
            routes = routes,
            selectedId = selectedRoute?.id,
            onSelect = {
                viewModel.selectRoute(it)
                showRoutePicker = false
            },
            onDismiss = { showRoutePicker = false },
        )
    }

    interruptedId?.let { id ->
        InterruptedRunDialog(
            onResume = {
                withPermission {
                    LocationTrackingService.restore(context, id)
                    viewModel.onInterruptedResumed()
                }
            },
            onFinalize = viewModel::finalizeInterrupted,
            onDiscard = viewModel::discardInterrupted,
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ghi hoạt động") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (state.status == TrackingStatus.IDLE) {
            IdleContent(
                padding = padding,
                activities = activities,
                selectedRoute = selectedRoute,
                beaconSharing = beacon.sharing,
                beaconCode = beacon.code,
                hasPermission = hasPermission,
                onPickRoute = { showRoutePicker = true },
                onToggleBeacon = viewModel::setBeaconSharing,
                onStart = { withPermission { LocationTrackingService.start(context) } },
                onActivityClick = onActivityClick,
                context = context,
            )
        } else {
            ActiveContent(
                padding = padding,
                state = state,
                liveTrace = liveTrace,
                selectedRoute = selectedRoute,
                beaconSharing = beacon.sharing,
                beaconCode = beacon.code,
                voiceEnabled = voiceEnabled,
                onToggleVoice = { voiceEnabled = !voiceEnabled },
                onToggleBeacon = viewModel::setBeaconSharing,
                onPause = { LocationTrackingService.pause(context) },
                onResume = { LocationTrackingService.resume(context) },
                onStop = { LocationTrackingService.stop(context) },
            )
        }
    }
}

@Composable
private fun IdleContent(
    padding: PaddingValues,
    activities: List<Activity>,
    selectedRoute: Route?,
    beaconSharing: Boolean,
    beaconCode: String?,
    hasPermission: Boolean,
    onPickRoute: () -> Unit,
    onToggleBeacon: (Boolean) -> Unit,
    onStart: () -> Unit,
    onActivityClick: (String) -> Unit,
    context: android.content.Context,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            RecordButton(onClick = onStart)
            if (!hasPermission) {
                Text(
                    "Cần quyền vị trí để ghi GPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        item {
            FlatCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        StravaLabel("Dẫn đường")
                        Text(
                            selectedRoute?.name ?: "Không chọn route",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    TextButton(onClick = onPickRoute) { Text(if (selectedRoute == null) "Chọn" else "Đổi") }
                }
                androidx.compose.material3.HorizontalDivider(
                    Modifier.padding(vertical = Spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        StravaLabel("Chia sẻ trực tiếp")
                        Text(
                            if (beaconSharing && beaconCode != null) "Mã $beaconCode" else "Tắt",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Switch(checked = beaconSharing, onCheckedChange = onToggleBeacon)
                }
            }
        }
        if (!BatteryOptimization.isIgnoringOptimizations(context)) {
            item { BatteryOptimizationCard(context = context) }
        }
        item {
            SectionHeader(
                title = "Hoạt động gần đây",
                subtitle = if (activities.isEmpty()) null else "${activities.size} buổi",
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
        if (activities.isEmpty()) {
            item {
                EmptyState(
                    title = "Chưa có buổi tập nào",
                    message = "Nhấn nút GHI để bắt đầu buổi chạy đầu tiên.",
                )
            }
        } else {
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

@Composable
private fun ActiveContent(
    padding: PaddingValues,
    state: TrackingState,
    liveTrace: List<GeoPoint>,
    selectedRoute: Route?,
    beaconSharing: Boolean,
    beaconCode: String?,
    voiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onToggleBeacon: (Boolean) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val paused = state.status == TrackingStatus.PAUSED
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Đồng hồ lớn
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            StravaLabel(if (paused) "Đã tạm dừng" else "Thời gian")
            Text(
                formatClock(state.elapsedSeconds),
                style = MaterialTheme.typography.displayLarge,
                color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }

        FlatCard {
            StatStrip(
                listOf(
                    StatCell("Quãng đường", formatDistanceKm(state.distanceMeters)),
                    StatCell("Pace", formatPace(state.avgPaceSecPerKm)),
                    StatCell("Nhịp tim", state.liveHeartRateBpm?.toString() ?: "—"),
                ),
            )
            androidx.compose.material3.HorizontalDivider(
                Modifier.padding(vertical = Spacing.sm),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            StatStrip(
                listOf(
                    StatCell("Độ cao lên", "${state.elevationGainMeters.roundToInt()} m"),
                    StatCell("Độ cao xuống", "${state.elevationLossMeters.roundToInt()} m"),
                    StatCell("Điểm GPS", state.pointCount.toString()),
                ),
            )
        }

        val traceLatLngs = remember(liveTrace) { liveTrace.map { LatLng(it.latitude, it.longitude) } }
        val routeLatLngs = remember(selectedRoute) {
            selectedRoute?.polyline?.map { LatLng(it.latitude, it.longitude) }.orEmpty()
        }
        val current = state.lastLatitude?.let { lat ->
            state.lastLongitude?.let { lng -> LatLng(lat, lng) }
        }
        LiveTrackingMap(
            trace = traceLatLngs,
            plannedRoute = routeLatLngs,
            current = current,
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )

        if (state.navRouteName != null) {
            NavigationCard(
                routeName = state.navRouteName!!,
                instruction = state.navInstruction,
                distanceMeters = state.navDistanceMeters,
                offRoute = state.navOffRoute,
                stepIndex = state.navStepIndex,
                stepCount = state.navStepCount,
            )
            TextButton(onClick = onToggleVoice) {
                Text(if (voiceEnabled) "🔊 Tắt đọc chỉ đường" else "🔈 Bật đọc chỉ đường")
            }
        }

        if (beaconSharing && beaconCode != null) {
            Text(
                "Đang chia sẻ vị trí · mã $beaconCode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (paused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Icon(Icons.Filled.PlayArrow, null); Text(" Tiếp tục") }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text("Tạm dừng") }
            }
            Button(
                onClick = onStop,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Icon(Icons.Filled.Close, null); Text(" Kết thúc") }
        }
    }
}

@Composable
private fun RecordButton(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(132.dp).padding(vertical = Spacing.sm),
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Bắt đầu",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    "GHI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
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

@Composable
private fun InterruptedRunDialog(
    onResume: () -> Unit,
    onFinalize: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Buổi tập bị gián đoạn") },
        text = { Text("Ứng dụng bị dừng khi đang ghi. Bạn muốn làm gì với dữ liệu đã lưu?") },
        confirmButton = { TextButton(onClick = onResume) { Text("Tiếp tục ghi") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextButton(onClick = onFinalize) { Text("Kết thúc") }
                TextButton(onClick = onDiscard) { Text("Xoá") }
            }
        },
    )
}

@Composable
private fun BatteryOptimizationCard(context: android.content.Context) {
    FlatCard {
        Text(
            "Điện thoại có thể tự tắt việc ghi GPS khi khoá màn hình.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            Modifier.padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(onClick = { BatteryOptimization.requestIgnoreOptimizations(context) }) {
                Text("Tắt tối ưu hoá pin")
            }
            if (BatteryOptimization.hasAggressiveOem()) {
                OutlinedButton(onClick = { BatteryOptimization.openOemAutoStartSettings(context) }) {
                    Text("Tự khởi động")
                }
            }
        }
    }
}

@Composable
private fun NavigationCard(
    routeName: String,
    instruction: String?,
    distanceMeters: Double?,
    offRoute: Boolean,
    stepIndex: Int,
    stepCount: Int,
) {
    val container = if (offRoute) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    androidx.compose.material3.Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md)) {
            StravaLabel(routeName)
            Text(
                when {
                    offRoute -> "⚠ Đã đi chệch route"
                    instruction != null -> instruction
                    else -> "Bám theo route"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            val meta = buildList {
                distanceMeters?.let { add("còn ${it.roundToInt()} m") }
                if (stepCount > 0) add("bước ${(stepIndex + 1).coerceAtMost(stepCount)}/$stepCount")
            }
            if (meta.isNotEmpty()) {
                Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RoutePickerDialog(
    routes: List<Route>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn route") },
        text = {
            if (routes.isEmpty()) {
                Text("Chưa có route. Dựng route ở màn Hồ sơ → Routes đã lưu.")
            } else {
                LazyColumn {
                    item {
                        TextButton(onClick = { onSelect(null) }) { Text("Không dẫn đường") }
                    }
                    items(routes, key = { it.id }) { route ->
                        TextButton(onClick = { onSelect(route.id) }) {
                            Text(
                                (if (route.id == selectedId) "✓ " else "") +
                                    "${route.name} · ${route.distanceMeters.roundToInt() / 1000.0} km",
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}
