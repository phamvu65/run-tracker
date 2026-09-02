package com.example.runtracker.ui.tracking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.example.runtracker.tracking.TrackingState
import com.example.runtracker.tracking.TrackingStatus
import com.example.runtracker.tracking.LocationTrackingService
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.components.StatTile
import com.example.runtracker.ui.theme.Spacing
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

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
        topBar = { TopAppBar(title = { Text("Ghi hoạt động") }) },
    ) { padding ->
        if (state.status == TrackingStatus.IDLE) {
            IdleContent(
                padding = padding,
                state = state,
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
    state: TrackingState,
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
            OutlinedButton(onClick = onPickRoute, modifier = Modifier.fillMaxWidth()) {
                Text(selectedRoute?.let { "Route: ${it.name}" } ?: "Chọn route để dẫn đường")
            }
        }
        item {
            BeaconCard(sharing = beaconSharing, code = beaconCode, onToggle = onToggleBeacon)
        }
        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  Bắt đầu chạy", style = MaterialTheme.typography.titleMedium)
            }
            if (!hasPermission) {
                Text(
                    "Cần quyền vị trí để ghi GPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
        if (!BatteryOptimization.isIgnoringOptimizations(context)) {
            item { BatteryOptimizationCard(context = context) }
        }
        item {
            SectionHeader(
                title = "Lịch sử",
                subtitle = if (activities.isEmpty()) null else "${activities.size} buổi",
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
        if (activities.isEmpty()) {
            item {
                EmptyState(
                    title = "Chưa có buổi tập nào",
                    message = "Bấm \"Bắt đầu chạy\" để ghi buổi đầu tiên.",
                )
            }
        } else {
            items(activities, key = { it.id }) { activity ->
                HistoryCard(activity = activity, onClick = { onActivityClick(activity.id) })
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
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
            modifier = Modifier.fillMaxWidth().height(220.dp),
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

        HeroMetric(
            label = if (state.status == TrackingStatus.PAUSED) "ĐÃ TẠM DỪNG · QUÃNG ĐƯỜNG" else "QUÃNG ĐƯỜNG",
            value = formatDistanceKm(state.distanceMeters),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatTile("Thời gian", formatClock(state.elapsedSeconds), Modifier.weight(1f))
            StatTile("Pace", formatPace(state.avgPaceSecPerKm), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatTile(
                "Nhịp tim",
                state.liveHeartRateBpm?.let { "$it" } ?: "—",
                Modifier.weight(1f),
            )
            StatTile(
                "Độ cao +/-",
                "${state.elevationGainMeters.roundToInt()}/${state.elevationLossMeters.roundToInt()}",
                Modifier.weight(1f),
            )
        }

        BeaconCard(sharing = beaconSharing, code = beaconCode, onToggle = onToggleBeacon)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when (state.status) {
                TrackingStatus.TRACKING -> OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Tạm dừng") }

                else -> FilledTonalButton(
                    onClick = onResume,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Tiếp tục") }
            }
            Button(
                onClick = onStop,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Kết thúc") }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String) {
    ElevatedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCard(
    activity: Activity,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(
                "%s · %s".format(
                    formatDistanceKm(activity.distanceMeters),
                    formatClock(activity.movingTime.inWholeSeconds),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${activity.type} · ${activity.startTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                "Điện thoại có thể tự tắt việc ghi GPS khi khoá màn hình.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
}

@Composable
private fun BeaconCard(
    sharing: Boolean,
    code: String?,
    onToggle: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Chia sẻ vị trí trực tiếp", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (sharing) {
                            "Người thân có thể theo dõi buổi chạy này theo thời gian thực."
                        } else {
                            "Bật để cho người thân theo dõi vị trí khi bạn đang chạy."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = sharing, onCheckedChange = onToggle)
            }
            if (sharing && code != null) {
                Text(
                    "Mã chia sẻ",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Text(code, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Người theo dõi: mở app → Hồ sơ → \"Theo dõi trực tiếp\", nhập mã này.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md)) {
            Text(routeName, style = MaterialTheme.typography.labelMedium)
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
