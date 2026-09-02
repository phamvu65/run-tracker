package com.example.runtracker.ui.beacon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.domain.model.BeaconSnapshot
import com.example.runtracker.domain.model.BeaconStatus
import com.example.runtracker.ui.common.PathMap
import com.example.runtracker.ui.components.LabeledValue
import com.example.runtracker.ui.theme.Spacing
import com.google.android.gms.maps.model.LatLng
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconViewerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BeaconViewerViewModel = hiltViewModel(),
) {
    val code by viewModel.code.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()
    val status by viewModel.status.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Theo dõi trực tiếp") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (code == null) {
                CodeEntry(
                    input = viewModel.input,
                    error = viewModel.error,
                    onInputChange = viewModel::onInputChange,
                    onFollow = viewModel::follow,
                )
            } else {
                Following(
                    code = code!!,
                    snapshot = snapshot,
                    status = status,
                    onStop = viewModel::stopFollowing,
                )
            }
        }
    }
}

@Composable
private fun CodeEntry(
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onFollow: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            "Nhập mã beacon mà người chạy chia sẻ để xem vị trí của họ theo thời gian thực.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Mã beacon") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onFollow, modifier = Modifier.fillMaxWidth()) { Text("Theo dõi") }
    }
}

@Composable
private fun Following(
    code: String,
    snapshot: BeaconSnapshot?,
    status: BeaconStatus,
    onStop: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        val trail = remember(snapshot?.trail) {
            snapshot?.trail?.map { LatLng(it.latitude, it.longitude) }.orEmpty()
        }
        if (trail.isNotEmpty()) {
            PathMap(latLngs = trail, modifier = Modifier.fillMaxWidth().height(280.dp))
        } else {
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                Text(
                    "Đang chờ tín hiệu vị trí…",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mã $code", style = MaterialTheme.typography.titleMedium)
                StatusChip(status)
            }

            val latest = snapshot?.latest
            if (latest != null) {
                LabeledValue("Quãng đường", formatDistanceKm(latest.distanceMeters))
                LabeledValue("Thời gian", formatClock(latest.elapsedSeconds))
                LabeledValue(
                    "Cập nhật lúc",
                    latest.timestamp.atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FORMAT),
                )
                if (latest.paused) {
                    Text("Người chạy đang tạm dừng.", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("Dừng theo dõi")
            }
        }
    }
}

@Composable
private fun StatusChip(status: BeaconStatus) {
    val (label, color) = when (status) {
        BeaconStatus.LIVE -> "● Trực tiếp" to Color(0xFF2E7D32)
        BeaconStatus.STALE -> "● Chậm cập nhật" to Color(0xFFF9A825)
        BeaconStatus.ENDED -> "■ Đã kết thúc" to MaterialTheme.colorScheme.onSurfaceVariant
        BeaconStatus.WAITING -> "○ Đang chờ" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
    )
}
