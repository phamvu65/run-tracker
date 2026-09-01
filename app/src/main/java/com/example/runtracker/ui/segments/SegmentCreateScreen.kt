package com.example.runtracker.ui.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatDistanceKm
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.CameraUpdateFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentCreateScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SegmentCreateViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.savedSegmentId) {
        viewModel.savedSegmentId?.let {
            viewModel.consumeSaved()
            onCreated(it)
        }
    }

    val total = viewModel.totalDistanceMeters
    var range by remember(total) { mutableStateOf(0f..total) }
    var name by remember { mutableStateOf("") }

    val fullLine = remember(viewModel.routePoints) {
        viewModel.routePoints.map { LatLng(it.latitude, it.longitude) }
    }
    val subLine = remember(range, viewModel.routePoints) {
        viewModel.subRange(range.start, range.endInclusive).map { LatLng(it.latitude, it.longitude) }
    }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(fullLine) {
        if (fullLine.size >= 2) {
            val bounds = LatLngBounds.builder().apply { fullLine.forEach(::include) }.build()
            runCatching { cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 96)) }
        } else if (fullLine.size == 1) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(fullLine.first(), 15f)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tạo segment") },
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
            ) {
                if (fullLine.size >= 2) {
                    Polyline(points = fullLine, color = Color.Gray, width = 8f)
                }
                if (subLine.size >= 2) {
                    Polyline(points = subLine, color = MaterialTheme.colorScheme.primary, width = 14f)
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Đoạn: ${formatDistanceKm(range.start.toDouble())} → " +
                        "${formatDistanceKm(range.endInclusive.toDouble())} " +
                        "(${formatDistanceKm((range.endInclusive - range.start).toDouble())})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (total > 0f) {
                    RangeSlider(
                        value = range,
                        onValueChange = { range = it },
                        valueRange = 0f..total,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên segment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.save(name.trim(), range.start, range.endInclusive) },
                    enabled = name.isNotBlank() && !viewModel.saving &&
                        (range.endInclusive - range.start) >= 100f,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (viewModel.saving) "Đang lưu…" else "Lưu segment") }
            }
        }
    }
}
