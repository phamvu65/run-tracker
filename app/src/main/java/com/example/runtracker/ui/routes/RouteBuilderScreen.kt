package com.example.runtracker.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.TravelMode
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

private val DEFAULT_CAMERA = LatLng(10.7769, 106.7009) // TP.HCM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteBuilderScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RouteBuilderViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.savedRouteId) {
        viewModel.savedRouteId?.let {
            viewModel.consumeSaved()
            onSaved(it)
        }
    }

    var showNameDialog by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CAMERA, 13f)
    }

    val tapped = viewModel.tappedPoints
    val planned = viewModel.planned
    val previewLine = remember(tapped, planned) {
        (planned?.polyline ?: tapped).map { LatLng(it.latitude, it.longitude) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Dựng route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxWidth().weight(1f),
                cameraPositionState = cameraPositionState,
                uiSettings = remember { MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false) },
                onMapClick = { viewModel.addPoint(GeoPoint(it.latitude, it.longitude)) },
            ) {
                tapped.forEachIndexed { i, p ->
                    Marker(
                        state = rememberMarkerState(key = "wp$i", position = LatLng(p.latitude, p.longitude)),
                        title = "Điểm ${i + 1}",
                    )
                }
                if (previewLine.size >= 2) {
                    Polyline(points = previewLine, color = MaterialTheme.colorScheme.primary, width = 10f)
                }
            }

            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        planned != null -> "${formatDistanceKm(planned.distanceMeters)}" +
                            if (planned.snappedToRoads) " (bám đường)" else " (đường thẳng)"
                        tapped.size >= 2 -> "${tapped.size} điểm — bấm \"Tính đường\""
                        else -> "Chạm vào bản đồ để thêm điểm"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = viewModel.mode == TravelMode.WALKING,
                        onClick = { viewModel.selectMode(TravelMode.WALKING) },
                        label = { Text("Đi bộ") },
                    )
                    FilterChip(
                        selected = viewModel.mode == TravelMode.CYCLING,
                        onClick = { viewModel.selectMode(TravelMode.CYCLING) },
                        label = { Text("Đạp xe") },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::undo, enabled = tapped.isNotEmpty()) { Text("Lùi") }
                    OutlinedButton(onClick = viewModel::clear, enabled = tapped.isNotEmpty()) { Text("Xoá hết") }
                    Button(
                        onClick = viewModel::computeRoute,
                        enabled = tapped.size >= 2 && !viewModel.loading,
                    ) { Text(if (viewModel.loading) "Đang tính…" else "Tính đường") }
                }

                Button(
                    onClick = { showNameDialog = true },
                    enabled = planned != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Lưu route") }
            }
        }
    }

    if (showNameDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Tên route") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Ví dụ: Vòng hồ Bán Nguyệt") },
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    viewModel.save(name.trim())
                    showNameDialog = false
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Huỷ") } },
        )
    }
}
