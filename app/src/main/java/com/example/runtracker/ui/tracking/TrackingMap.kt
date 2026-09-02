package com.example.runtracker.ui.tracking

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.ui.common.MapLine
import com.example.runtracker.ui.common.MapMarker
import com.example.runtracker.ui.common.fitToPoints
import com.example.runtracker.ui.common.rememberOsmMapView
import com.example.runtracker.ui.common.renderPath
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.util.GeoPoint as OsmPoint
import kotlin.coroutines.resume

/**
 * Bản đồ toàn màn hình cho màn Ghi. Lúc chờ (IDLE): hiện chấm vị trí của tôi + route đã chọn.
 * Lúc đang ghi: thêm trace đã đi và camera bám theo vị trí hiện tại.
 */
@Composable
fun TrackingMap(
    plannedRoute: List<GeoPoint>,
    trace: List<GeoPoint>,
    current: GeoPoint?,
    follow: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val hasPermission = remember { context.hasLocationPermission() }
    val mapView = rememberOsmMapView()

    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

    // Vị trí gần nhất để đặt camera khi chưa có fix nào.
    var lastKnown by remember { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(hasPermission) {
        if (hasPermission && lastKnown == null) {
            runCatching { fetchLastLocation(context) }.getOrNull()?.let { lastKnown = it }
        }
    }

    // Chấm "vị trí của tôi" của osmdroid (chỉ bật khi có quyền).
    val myLocationOverlay = remember(mapView, hasPermission) {
        if (hasPermission) {
            MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { disableFollowLocation() }
        } else {
            null
        }
    }
    DisposableEffect(myLocationOverlay) {
        myLocationOverlay?.enableMyLocation()
        onDispose { myLocationOverlay?.disableMyLocation() }
    }

    var cameraInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(current, lastKnown, plannedRoute, follow) {
        when {
            follow && current != null -> {
                if (!cameraInitialized) {
                    mapView.controller.setZoom(16.0)
                    cameraInitialized = true
                }
                mapView.controller.animateTo(OsmPoint(current.latitude, current.longitude))
            }
            !cameraInitialized && plannedRoute.size >= 2 -> {
                mapView.fitToPoints(plannedRoute, (32 * density).toInt())
                cameraInitialized = true
            }
            !cameraInitialized -> {
                (current ?: lastKnown)?.let {
                    mapView.controller.setZoom(16.0)
                    mapView.controller.setCenter(OsmPoint(it.latitude, it.longitude))
                    cameraInitialized = true
                }
            }
        }
    }

    val lines = buildList {
        if (plannedRoute.size >= 2) add(MapLine(plannedRoute, outline, widthDp = 3f))
        if (trace.size >= 2) add(MapLine(trace, primary, widthDp = 4.5f))
    }
    val markers = current?.let { listOf(MapMarker(it, "Vị trí hiện tại")) }.orEmpty()

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = {
            it.renderPath(
                lines = lines,
                markers = markers,
                onTap = null,
                density = density,
                extraOverlays = listOfNotNull(myLocationOverlay),
            )
        },
    )
}

@SuppressLint("MissingPermission")
private suspend fun fetchLastLocation(context: Context): GeoPoint? {
    if (!context.hasLocationPermission()) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc ->
                cont.resume(loc?.let { GeoPoint(it.latitude, it.longitude) })
            }
            .addOnFailureListener { cont.resume(null) }
    }
}
