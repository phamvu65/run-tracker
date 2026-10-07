package com.example.runtracker.ui.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runtracker.core.FeatureFlags
import com.example.runtracker.core.BatteryOptimization
import com.example.runtracker.core.formatClock
import com.example.runtracker.ui.theme.formatDistanceUnit
import com.example.runtracker.ui.theme.formatPaceUnit
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.core.isLocationEnabled
import com.example.runtracker.core.trackingPermissions
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Route
import com.example.runtracker.tracking.LocationTrackingService
import com.example.runtracker.tracking.TrackingState
import com.example.runtracker.tracking.TrackingStatus
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.components.StatStrip
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing

@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    onRunComplete: (activityId: String) -> Unit = {},
    onCreateRoute: () -> Unit = {},
    onEditRoute: (String) -> Unit = {},
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.tracking.collectAsState()
    val interruptedId by viewModel.interruptedActivityId.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val plannedType by viewModel.plannedType.collectAsState()
    val liveTrace by viewModel.liveTrace.collectAsState()
    val beacon by viewModel.beacon.collectAsState()
    val justFinishedId by viewModel.justFinishedActivityId.collectAsState()

    LaunchedEffect(justFinishedId) {
        justFinishedId?.let {
            viewModel.consumeJustFinishedActivity()
            onRunComplete(it)
        }
    }

    var showRoutePicker by remember { mutableStateOf(false) }
    var showSportPicker by remember { mutableStateOf(false) }
    var voiceEnabled by rememberSaveable { mutableStateOf(true) }

    val speak = rememberRouteVoice()
    var lastSpokenStep by remember { mutableIntStateOf(-1) }
    var lastOffRoute by remember { mutableStateOf(false) }
    var lastSpokenPolyline by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    LaunchedEffect(state.navStepIndex, state.navOffRoute, state.navRouteName, state.navPolyline, state.status, voiceEnabled) {
        if (state.status == TrackingStatus.IDLE) {
            lastSpokenStep = -1
            lastSpokenPolyline = emptyList()
            lastOffRoute = false
        }
        if (!voiceEnabled || state.status == TrackingStatus.IDLE || state.navRouteName == null) {
            return@LaunchedEffect
        }
        if (state.navOffRoute) {
            if (!lastOffRoute) speak("Đã đi chệch tuyến đường")
        } else if (state.navStepIndex != lastSpokenStep || state.navPolyline != lastSpokenPolyline) {
            state.navInstruction?.let { speak(it) }
            lastSpokenStep = state.navStepIndex
            lastSpokenPolyline = state.navPolyline
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

    val locationEnabled by rememberLocationEnabled()
    fun openLocationSettings() {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
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
            onCreate = { showRoutePicker = false; onCreateRoute() },
            onEdit = { showRoutePicker = false; onEditRoute(it) },
        )
    }

    if (showSportPicker) {
        SportPickerDialog(
            selected = plannedType,
            onSelect = {
                viewModel.setPlannedType(it)
                showSportPicker = false
            },
            onDismiss = { showSportPicker = false },
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

    var showNoMovementDialog by remember { mutableStateOf(false) }
    if (showNoMovementDialog) {
        NoMovementDialog(
            onContinue = { showNoMovementDialog = false },
            onDiscard = {
                showNoMovementDialog = false
                LocationTrackingService.discard(context)
            },
        )
    }

    val plannedRoute = if (state.isActive) state.navPolyline else selectedRoute?.polyline.orEmpty()
    val current = state.lastLatitude?.let { lat ->
        state.lastLongitude?.let { lng -> GeoPoint(lat, lng) }
    }
    // Lúc IDLE service chưa chạy nên `state.gpsSignalOk` không có ý nghĩa gì — dùng tín hiệu fix
    // thật từ chính bản đồ (osmdroid) để banner không "nói dối" là đã kết nối trước khi có fix.
    var idleGpsFix by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TrackingMap(
            plannedRoute = plannedRoute,
            trace = liveTrace,
            current = current,
            follow = state.status != TrackingStatus.IDLE,
            hasPermission = hasPermission,
            modifier = Modifier.fillMaxSize(),
            controlsAlignment = BiasAlignment(horizontalBias = 1f, verticalBias = -0.15f),
            onFixAvailable = { idleGpsFix = it },
        )

        // ---- Lớp phủ trên ----
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (state.status == TrackingStatus.IDLE &&
                !BatteryOptimization.isIgnoringOptimizations(context)
            ) {
                BatteryOptimizationBanner(context)
            }
            if (state.navRouteName != null) {
                if (state.navRerouting) Text("Đang tính đường quay lại lộ trình…")
                NavigationCard(
                    routeName = state.navRouteName!!,
                    instruction = state.navInstruction,
                    distanceMeters = state.navDistanceMeters,
                    offRoute = state.navOffRoute,
                    stepIndex = state.navStepIndex,
                    stepCount = state.navStepCount,
                )
                TextButton(onClick = { voiceEnabled = !voiceEnabled }) {
                    Text(if (voiceEnabled) "🔊 Tắt đọc chỉ đường" else "🔈 Bật đọc chỉ đường")
                }
            }
        }

        // ---- Bảng điều khiển dưới ----
        RecordPanel(
            state = state,
            hasPermission = hasPermission,
            locationEnabled = locationEnabled,
            hasIdleGpsFix = idleGpsFix,
            plannedType = plannedType,
            selectedRoute = selectedRoute,
            beaconSharing = beacon.sharing,
            beaconCode = beacon.code,
            onRequestPermission = { withPermission {} },
            onEnableLocation = ::openLocationSettings,
            onStart = {
                withPermission {
                    if (locationEnabled) LocationTrackingService.start(context) else openLocationSettings()
                }
            },
            onPause = { LocationTrackingService.pause(context) },
            onResume = { LocationTrackingService.resume(context) },
            onStop = {
                if (state.movingTimeSeconds == 0L) {
                    showNoMovementDialog = true
                } else {
                    LocationTrackingService.stop(context)
                }
            },
            onPickSport = { showSportPicker = true },
            onPickRoute = { showRoutePicker = true },
            onToggleBeacon = viewModel::setBeaconSharing,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Công tắc định vị hệ thống (khác quyền ứng dụng) — cập nhật qua broadcast
 * [LocationManager.PROVIDERS_CHANGED_ACTION] (bật/tắt từ Cài đặt nhanh không cần rời app) + lúc
 * quay lại màn (ON_RESUME, phòng khi broadcast bị OEM chặn nền).
 */
@Composable
private fun rememberLocationEnabled(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(context.isLocationEnabled()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                state.value = context.isLocationEnabled()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.value = context.isLocationEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

@Composable
private fun RecordPanel(
    state: TrackingState,
    hasPermission: Boolean,
    locationEnabled: Boolean,
    hasIdleGpsFix: Boolean,
    plannedType: ActivityType,
    selectedRoute: Route?,
    beaconSharing: Boolean,
    beaconCode: String?,
    onRequestPermission: () -> Unit,
    onEnableLocation: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onPickSport: () -> Unit,
    onPickRoute: () -> Unit,
    onToggleBeacon: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val idle = state.status == TrackingStatus.IDLE
    val paused = state.status == TrackingStatus.PAUSED

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Column(
            Modifier
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.sm, bottom = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.height(Spacing.md))

            GpsStrip(
                state = state,
                hasPermission = hasPermission,
                locationEnabled = locationEnabled,
                hasIdleGpsFix = hasIdleGpsFix,
                onRequestPermission = onRequestPermission,
                onEnableLocation = onEnableLocation,
            )
            if (paused && state.autoPaused) {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(Color(0xFF3A2E1B))
                        .padding(vertical = Spacing.sm, horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "⏸  Tự động tạm dừng — di chuyển tiếp để ghi lại",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFE2C08D),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))

            val stats = buildList {
                add(StatCell("Thời gian", formatClock(state.elapsedSeconds)))
                add(
                    StatCell(
                        if (plannedType == ActivityType.CYCLING) "Tốc độ" else "Nhịp độ",
                        formatPaceUnit(state.avgPaceSecPerKm),
                    ),
                )
                add(StatCell("Quãng đường", formatDistanceUnit(state.distanceMeters)))
                state.liveHeartRateBpm?.takeIf { FeatureFlags.HEART_RATE_INTEGRATION }?.let { add(StatCell("Nhịp tim", "$it")) }
                state.liveCadenceSpm?.let { add(StatCell("Cadence", "$it spm")) }
            }
            StatStrip(stats)
            Spacer(Modifier.height(Spacing.xl))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (idle) {
                    CircleAction(
                        label = plannedType.vi(),
                        onClick = onPickSport,
                        content = { Text(plannedType.glyph(), style = MaterialTheme.typography.titleLarge) },
                    )
                    BigButton(
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onStart,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Bắt đầu",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    CircleAction(
                        label = selectedRoute?.name ?: "Lộ trình",
                        onClick = onPickRoute,
                        content = {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Chọn lộ trình",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                    )
                } else {
                    CircleAction(
                        label = "Kết thúc",
                        onClick = onStop,
                        bg = MaterialTheme.colorScheme.errorContainer,
                        content = { StopGlyph(MaterialTheme.colorScheme.onErrorContainer) },
                    )
                    BigButton(
                        color = MaterialTheme.colorScheme.primary,
                        onClick = if (paused) onResume else onPause,
                    ) {
                        if (paused) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Tiếp tục",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp),
                            )
                        } else {
                            PauseGlyph(MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    CircleAction(
                        label = if (beaconSharing) "Đang chia sẻ" else "Chia sẻ",
                        onClick = { onToggleBeacon(!beaconSharing) },
                        bg = if (beaconSharing) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        content = { Text("📡", style = MaterialTheme.typography.titleMedium) },
                    )
                }
            }

            if (beaconSharing && beaconCode != null) {
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Đang chia sẻ vị trí · mã $beaconCode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (idle) {
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Chia sẻ vị trí trực tiếp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Switch(checked = beaconSharing, onCheckedChange = onToggleBeacon)
                }
            }
        }
    }
}

/**
 * 4 trạng thái: thiếu quyền vị trí / công tắc định vị hệ thống đang tắt / đang chờ (hoặc mất) tín
 * hiệu GPS / đã có tín hiệu tốt. Lúc TRACKING dựa vào [TrackingState.gpsSignalOk] + số điểm đã
 * nhận (service tự theo dõi độ trễ fix). Lúc IDLE service chưa chạy nên không có gì để hỏi — dùng
 * [hasIdleGpsFix] (tín hiệu fix thật qua FusedLocation, xem [TrackingMap]) thay vì mặc định coi là
 * "ổn". Lúc PAUSED giữ nguyên coi ổn vì buổi TRACKING trước đó chắc chắn đã có fix.
 */
@Composable
private fun GpsStrip(
    state: TrackingState,
    hasPermission: Boolean,
    locationEnabled: Boolean,
    hasIdleGpsFix: Boolean,
    onRequestPermission: () -> Unit,
    onEnableLocation: () -> Unit,
) {
    val waitingForFix = when (state.status) {
        TrackingStatus.TRACKING -> state.pointCount == 0 || !state.gpsSignalOk
        TrackingStatus.IDLE -> !hasIdleGpsFix
        TrackingStatus.PAUSED -> false
    }
    val bg: Color
    val fg: Color
    val label: String
    val onClick: (() -> Unit)?
    when {
        !hasPermission -> {
            bg = MaterialTheme.colorScheme.errorContainer
            fg = MaterialTheme.colorScheme.onErrorContainer
            label = "⚠  Cần quyền vị trí — chạm để cấp"
            onClick = onRequestPermission
        }
        !locationEnabled -> {
            bg = MaterialTheme.colorScheme.errorContainer
            fg = MaterialTheme.colorScheme.onErrorContainer
            label = "📍  GPS đang tắt — chạm để bật"
            onClick = onEnableLocation
        }
        waitingForFix -> {
            bg = Color(0xFF3A2E1B)
            fg = Color(0xFFE2C08D)
            label = "📡  Đang chờ tín hiệu GPS…"
            onClick = null
        }
        else -> {
            bg = Color(0xFF1B3A1E)
            fg = Color(0xFF9BE29E)
            label = "📶  Đã kết nối GPS"
            onClick = null
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(vertical = Spacing.sm, horizontal = Spacing.md),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun BigButton(
    color: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        modifier = Modifier.size(76.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun CircleAction(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    Column(
        modifier.width(84.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Surface(onClick = onClick, shape = CircleShape, color = bg, modifier = Modifier.size(52.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PauseGlyph(color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) {
            Box(
                Modifier
                    .size(width = 7.dp, height = 26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun StopGlyph(color: Color) {
    Box(
        Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

@Composable
private fun BatteryOptimizationBanner(context: android.content.Context) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                "Điện thoại có thể tự tắt việc ghi GPS khi khoá màn hình.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TextButton(onClick = { BatteryOptimization.requestIgnoreOptimizations(context) }) {
                    Text("Tắt tối ưu hoá pin")
                }
                if (BatteryOptimization.hasAggressiveOem()) {
                    TextButton(onClick = { BatteryOptimization.openOemAutoStartSettings(context) }) {
                        Text("Tự khởi động")
                    }
                }
            }
        }
    }
}

private fun ActivityType.vi(): String = when (this) {
    ActivityType.RUNNING -> "Chạy bộ"
    ActivityType.CYCLING -> "Đạp xe"
    ActivityType.WALKING -> "Đi bộ"
    ActivityType.OTHER -> "Khác"
}

private fun ActivityType.glyph(): String = when (this) {
    ActivityType.RUNNING -> "🏃"
    ActivityType.CYCLING -> "🚴"
    ActivityType.WALKING -> "🚶"
    ActivityType.OTHER -> "🏋"
}

@Composable
private fun SportPickerDialog(
    selected: ActivityType,
    onSelect: (ActivityType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Loại hoạt động") },
        text = {
            Column {
                listOf(ActivityType.RUNNING, ActivityType.WALKING, ActivityType.CYCLING).forEach { t ->
                    TextButton(
                        onClick = { onSelect(t) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            (if (t == selected) "✓  " else "") + "${t.glyph()}  ${t.vi()}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
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

/**
 * Chặn "Kết thúc" khi chưa ghi nhận di chuyển gì (xem điều kiện gọi ở [RecordPanel]'s `onStop`) —
 * một buổi 0 quãng đường không có ý nghĩa để lưu vào lịch sử. Không có nút "Kết thúc" bình thường:
 * chỉ được tiếp tục ghi, hoặc xoá hẳn buổi này.
 */
@Composable
private fun NoMovementDialog(onContinue: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text("Chưa ghi nhận di chuyển") },
        text = { Text("Bạn chưa di chuyển trong buổi này nên chưa thể kết thúc. Tiếp tục ghi hoặc xoá buổi tập này.") },
        confirmButton = { TextButton(onClick = onContinue) { Text("Tiếp tục ghi") } },
        dismissButton = { TextButton(onClick = onDiscard) { Text("Xoá buổi tập") } },
    )
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
        MaterialTheme.colorScheme.surface
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        shadowElevation = 6.dp,
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
                distanceMeters?.let { add("còn ${it.toInt()} m") }
                if (stepCount > 0) add("bước ${(stepIndex + 1).coerceAtMost(stepCount)}/$stepCount")
            }
            if (meta.isNotEmpty()) {
                Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun RoutePickerDialog(
    routes: List<Route>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn lộ trình") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = { onSelect(null) }) { Text("Không dẫn đường") }
                TextButton(onClick = onCreate) { Text("Tạo lộ trình mới") }
                if (routes.isEmpty()) Text("Chưa có lộ trình đã lưu. Tạo lộ trình để bắt đầu.")
                routes.forEach { route ->
                    val canNavigate = route.snappedToRoads && route.travelMode != null
                    TextButton(onClick = {
                        if (canNavigate) onSelect(route.id) else onEdit(route.id)
                    }) {
                        Text(
                            (if (route.id == selectedId) "✓ " else "") +
                                "${route.name} · ${route.distanceMeters.toInt() / 1000.0} km" +
                                if (!canNavigate) " — Sửa và tính đường lại" else "",
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}
