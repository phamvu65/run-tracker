package com.example.runtracker.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.runtracker.domain.model.RoutePoint
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
 * Bản đồ vẽ polyline của route đã ghi. Camera fit toàn bộ điểm sau khi map load xong
 * (dùng onMapLoaded để tránh IllegalStateException khi map chưa có kích thước).
 */
@Composable
fun RouteMap(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val latLngs = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mapLoaded, latLngs) {
        if (!mapLoaded || latLngs.isEmpty()) return@LaunchedEffect
        if (latLngs.size == 1) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 16f))
        } else {
            val bounds = LatLngBounds.builder().apply { latLngs.forEach(::include) }.build()
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 96))
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = remember { MapProperties(mapType = MapType.NORMAL) },
        uiSettings = remember { MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false) },
        onMapLoaded = { mapLoaded = true },
    ) {
        if (latLngs.size >= 2) {
            Polyline(
                points = latLngs,
                color = MaterialTheme.colorScheme.primary,
                width = 12f,
            )
            Marker(
                state = rememberMarkerState(key = "start", position = latLngs.first()),
                title = "Bắt đầu",
            )
            Marker(
                state = rememberMarkerState(key = "end", position = latLngs.last()),
                title = "Kết thúc",
            )
        }
    }
}
