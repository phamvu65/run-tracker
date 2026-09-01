package com.example.runtracker.ui.tracking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatPace
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.core.trackingPermissions
import com.example.runtracker.tracking.LocationTrackingService
import com.example.runtracker.tracking.TrackingStatus
import kotlin.math.roundToInt

@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.tracking.collectAsState()
    val activities by viewModel.activities.collectAsState()

    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) LocationTrackingService.start(context)
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Ghi hoạt động", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        StatRow("Thời gian", formatClock(state.elapsedSeconds))
        StatRow("Quãng đường", "%.2f km".format(state.distanceMeters / 1000.0))
        StatRow("Pace", formatPace(state.avgPaceSecPerKm))
        StatRow("Độ cao +/-", "${state.elevationGainMeters.roundToInt()} / ${state.elevationLossMeters.roundToInt()} m")
        StatRow("Điểm GPS", state.pointCount.toString())

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state.status) {
                TrackingStatus.IDLE -> Button(
                    onClick = {
                        if (context.hasLocationPermission()) {
                            LocationTrackingService.start(context)
                        } else {
                            permissionLauncher.launch(trackingPermissions())
                        }
                    },
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

        Spacer(Modifier.height(24.dp))
        Text("Lịch sử (${activities.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(activities, key = { it.id }) { activity ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
