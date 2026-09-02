package com.example.runtracker.ui.tracking

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.runtracker.core.hasLocationPermission
import kotlin.coroutines.resume
import com.google.android.gms.location.LocationServices
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
 * Bản đồ toàn màn hình cho màn Ghi. Lúc chờ (IDLE): hiện chấm vị trí của tôi + route đã chọn.
 * Lúc đang ghi: thêm trace đã đi và camera bám theo vị trí hiện tại.
 */
@Composable
fun TrackingMap(
    plannedRoute: List<LatLng>,
    trace: List<LatLng>,
    current: LatLng?,
    follow: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasPermission = remember { context.hasLocationPermission() }
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    var cameraInitialized by remember { mutableStateOf(false) }

    // Lấy vị trí gần nhất một lần để đặt camera khi chưa có fix nào.
    var lastKnown by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(hasPermission) {
        if (hasPermission && lastKnown == null) {
            runCatching { fetchLastLocation(context) }.getOrNull()?.let { lastKnown = it }
        }
    }

    LaunchedEffect(mapLoaded, current, lastKnown, plannedRoute, follow) {
        if (!mapLoaded) return@LaunchedEffect
        when {
            follow && current != null -> {
                val update = CameraUpdateFactory.newLatLngZoom(current, 16f)
                if (cameraInitialized) cameraPositionState.animate(update)
                else { cameraPositionState.move(update); cameraInitialized = true }
            }
            !cameraInitialized && plannedRoute.size >= 2 -> {
                val bounds = LatLngBounds.builder().apply { plannedRoute.forEach(::include) }.build()
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                cameraInitialized = true
            }
            !cameraInitialized -> {
                (current ?: lastKnown)?.let {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 16f))
                    cameraInitialized = true
                }
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = remember(hasPermission) {
            MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = hasPermission)
        },
        uiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
            )
        },
        onMapLoaded = { mapLoaded = true },
    ) {
        if (plannedRoute.size >= 2) {
            Polyline(points = plannedRoute, color = MaterialTheme.colorScheme.outline, width = 10f)
        }
        if (trace.size >= 2) {
            Polyline(points = trace, color = MaterialTheme.colorScheme.primary, width = 14f)
        }
        current?.let {
            Marker(rememberMarkerState(key = "$it", position = it), title = "Vị trí hiện tại")
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun fetchLastLocation(context: android.content.Context): LatLng? {
    if (!context.hasLocationPermission()) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc ->
                cont.resume(loc?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener { cont.resume(null) }
    }
}
