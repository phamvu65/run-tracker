package com.example.runtracker.ui.tracking

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

/**
 * Bản đồ hiển thị trong lúc đang ghi hoạt động: route đã chọn (xám mờ), trace GPS đã đi
 * (màu chính) và vị trí hiện tại. Camera fit route lúc đầu rồi bám theo vị trí hiện tại.
 */
@Composable
fun LiveTrackingMap(
    trace: List<LatLng>,
    plannedRoute: List<LatLng>,
    current: LatLng?,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    var cameraInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(mapLoaded, current, plannedRoute) {
        if (!mapLoaded) return@LaunchedEffect
        if (!cameraInitialized && plannedRoute.size >= 2) {
            val bounds = LatLngBounds.builder().apply { plannedRoute.forEach(::include) }.build()
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 96))
            cameraInitialized = true
        } else if (current != null) {
            val update = CameraUpdateFactory.newLatLngZoom(current, 16f)
            if (cameraInitialized) {
                cameraPositionState.animate(update)
            } else {
                cameraPositionState.move(update)
                cameraInitialized = true
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = remember { MapProperties(mapType = MapType.NORMAL) },
        uiSettings = remember {
            MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
        },
        onMapLoaded = { mapLoaded = true },
    ) {
        if (plannedRoute.size >= 2) {
            Polyline(
                points = plannedRoute,
                color = MaterialTheme.colorScheme.outline,
                width = 10f,
            )
        }
        if (trace.size >= 2) {
            Polyline(
                points = trace,
                color = MaterialTheme.colorScheme.primary,
                width = 12f,
            )
        }
        current?.let {
            Marker(
                state = rememberMarkerState(key = "$it", position = it),
                title = "Vị trí hiện tại",
            )
        }
    }
}
