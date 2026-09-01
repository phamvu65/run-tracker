package com.example.runtracker.ui.tracking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.BatteryOptimization
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatPace
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.core.trackingPermissions
import com.example.runtracker.domain.model.Route
import com.example.runtracker.tracking.LocationTrackingService
import com.example.runtracker.tracking.TrackingStatus
import kotlin.math.roundToInt

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
    var showRoutePicker by remember { mutableStateOf(false) }

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

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Ghi hoạt động",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))

        if (state.status != TrackingStatus.IDLE && state.navRouteName != null) {
            NavigationCard(
                routeName = state.navRouteName!!,
                instruction = state.navInstruction,
                distanceMeters = state.navDistanceMeters,
                offRoute = state.navOffRoute,
                stepIndex = state.navStepIndex,
                stepCount = state.navStepCount,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (state.status == TrackingStatus.IDLE) {
            OutlinedButton(
                onClick = { showRoutePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedRoute?.let { "Route: ${it.name}" } ?: "Chọn route để dẫn đường")
            }
            Spacer(Modifier.height(12.dp))
        }

        StatRow("Thời gian", formatClock(state.elapsedSeconds))
        StatRow("Quãng đường", "%.2f km".format(state.distanceMeters / 1000.0))
        StatRow("Pace", formatPace(state.avgPaceSecPerKm))
        state.liveHeartRateBpm?.let { StatRow("Nhịp tim", "$it bpm") }
        StatRow(
            "Độ cao +/-",
            "${state.elevationGainMeters.roundToInt()} / ${state.elevationLossMeters.roundToInt()} m",
        )
        StatRow("Điểm GPS", state.pointCount.toString())

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state.status) {
                TrackingStatus.IDLE -> Button(
                    onClick = { withPermission { LocationTrackingService.start(context) } },
                ) { Text("Bắt đầu") }

                TrackingStatus.TRACKING -> {
                    OutlinedButton(onClick = { LocationTrackingService.pause(context) }) {
                        Text("Tạm dừng")
                    }
                    Button(onClick = { LocationTrackingService.stop(context) }) { Text("Kết thúc") }
                }

                TrackingStatus.PAUSED -> {
                    OutlinedButton(onClick = { LocationTrackingService.resume(context) }) {
                        Text("Tiếp tục")
                    }
                    Button(onClick = { LocationTrackingService.stop(context) }) { Text("Kết thúc") }
                }
            }
        }

        if (!hasPermission && state.status == TrackingStatus.IDLE) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Cần quyền vị trí để ghi GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (state.status == TrackingStatus.IDLE && !BatteryOptimization.isIgnoringOptimizations(context)) {
            Spacer(Modifier.height(12.dp))
            BatteryOptimizationCard(context = context)
        }

        Spacer(Modifier.height(24.dp))
        Text("Lịch sử (${activities.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(activities, key = { it.id }) { activity ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onActivityClick(activity.id) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        "%.2f km · %s".format(
                            activity.distanceMeters / 1000.0,
                            formatClock(activity.movingTime.inWholeSeconds),
                        ),
                    )
                    Text(
                        "${activity.type} · ${activity.startTime}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
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
        text = {
            Text(
                "Ứng dụng bị dừng khi đang ghi. Bạn muốn làm gì với dữ liệu đã lưu?",
            )
        },
        confirmButton = {
            TextButton(onClick = onResume) { Text("Tiếp tục ghi") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onFinalize) { Text("Kết thúc") }
                TextButton(onClick = onDiscard) { Text("Xoá") }
            }
        },
    )
}

@Composable
private fun BatteryOptimizationCard(context: android.content.Context) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Điện thoại có thể tự tắt việc ghi GPS khi khoá màn hình.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        MaterialTheme.colorScheme.primaryContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(routeName, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
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

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
