package com.example.runtracker.ui.tracking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.ui.common.MapControls
import com.example.runtracker.ui.common.MapGuideLineColor
import com.example.runtracker.ui.common.MapLine
import com.example.runtracker.ui.common.MapMarker
import com.example.runtracker.ui.common.MapStyle
import com.example.runtracker.ui.common.fitToPoints
import com.example.runtracker.ui.common.lastKnownLocation
import com.example.runtracker.ui.common.rememberOsmMapView
import com.example.runtracker.ui.common.renderPath
import com.example.runtracker.ui.common.startFinishMarkers
import com.example.runtracker.ui.common.tileSourceFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.util.GeoPoint as OsmPoint

/**
 * Bản đồ toàn màn hình cho màn Ghi. Lúc chờ (IDLE): hiện chấm vị trí của tôi + route đã chọn.
 * Lúc đang ghi: thêm trace đã đi và camera bám theo vị trí hiện tại.
 * [controlsPadding] chừa lề cho cụm nút để không bị bảng điều khiển dưới che.
 */
@Composable
fun TrackingMap(
    plannedRoute: List<GeoPoint>,
    trace: List<GeoPoint>,
    current: GeoPoint?,
    follow: Boolean,
    modifier: Modifier = Modifier,
    controlsAlignment: Alignment = Alignment.BottomEnd,
    controlsPadding: PaddingValues = PaddingValues(12.dp),
    /** Báo (lặp lại mỗi giây) đã bắt được vị trí GPS thật hay chưa — dùng cho banner trạng thái lúc IDLE. */
    onFixAvailable: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val hasPermission = remember { context.hasLocationPermission() }
    val mapView = rememberOsmMapView()

    val outline = MapGuideLineColor
    val primary = MaterialTheme.colorScheme.primary

    var style by rememberSaveable { mutableStateOf(MapStyle.STREET) }
    LaunchedEffect(mapView, style) { mapView.setTileSource(tileSourceFor(style)) }

    // Vị trí gần nhất để đặt camera khi chưa có fix nào.
    var lastKnown by remember { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(hasPermission) {
        if (hasPermission && lastKnown == null) {
            lastKnownLocation(context)?.let { lastKnown = it }
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

    // `current` (điểm GPS service đã ghi) là fix đáng tin nhất khi đang ghi; lúc IDLE service
    // chưa chạy nên hỏi trực tiếp osmdroid overlay — poll vì đây là getter thường, không phải Flow.
    LaunchedEffect(myLocationOverlay, current) {
        if (current != null) {
            onFixAvailable(true)
            return@LaunchedEffect
        }
        if (myLocationOverlay == null) {
            onFixAvailable(false)
            return@LaunchedEffect
        }
        while (isActive) {
            onFixAvailable(myLocationOverlay.myLocation != null)
            delay(1_000)
        }
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
        // Mũi tên trên route đã chọn để biết chạy theo chiều nào.
        if (plannedRoute.size >= 2) add(MapLine(plannedRoute, outline, widthDp = 3.5f, showDirection = true))
        if (trace.size >= 2) add(MapLine(trace, primary, widthDp = 4.5f))
    }
    val markers = buildList {
        addAll(startFinishMarkers(plannedRoute))
        current?.let { add(MapMarker(it, "Vị trí hiện tại")) }
    }

    val recenter: (() -> Unit)? = if (hasPermission) {
        {
            val fix = current?.let { OsmPoint(it.latitude, it.longitude) }
                ?: myLocationOverlay?.myLocation
                ?: lastKnown?.let { OsmPoint(it.latitude, it.longitude) }
            fix?.let {
                mapView.controller.animateTo(it)
                mapView.controller.setZoom(16.0)
            }
        }
    } else {
        null
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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
        MapControls(
            style = style,
            onToggleStyle = { style = if (style == MapStyle.STREET) MapStyle.SATELLITE else MapStyle.STREET },
            onRecenter = recenter,
            modifier = Modifier
                .align(controlsAlignment)
                .padding(controlsPadding),
        )
    }
}
