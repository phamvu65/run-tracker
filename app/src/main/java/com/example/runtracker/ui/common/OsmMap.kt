package com.example.runtracker.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runtracker.domain.model.GeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint as OsmPoint

/** Một đường vẽ trên bản đồ. `widthDp` là bề rộng nét theo dp (nhân với mật độ khi vẽ). */
data class MapLine(val points: List<GeoPoint>, val color: Color, val widthDp: Float = 4f)

/** Một điểm mốc trên bản đồ. */
data class MapMarker(val point: GeoPoint, val title: String? = null)

private fun GeoPoint.toOsm() = OsmPoint(latitude, longitude)

/**
 * `MapView` osmdroid gắn với vòng đời Compose: onResume/onPause theo lifecycle,
 * onDetach khi rời khỏi composition.
 */
@Composable
fun rememberOsmMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setUseDataConnection(true)
            isTilesScaledToDpi = true
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    return mapView
}

/** Vẽ lại toàn bộ overlay (copyright + tap + đường + mốc + overlay phụ). Gọi trong `AndroidView.update`. */
fun MapView.renderPath(
    lines: List<MapLine>,
    markers: List<MapMarker>,
    onTap: ((GeoPoint) -> Unit)?,
    density: Float,
    extraOverlays: List<Overlay> = emptyList(),
) {
    overlays.clear()
    overlays.add(CopyrightOverlay(context))

    if (onTap != null) {
        overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: OsmPoint): Boolean {
                    onTap(GeoPoint(p.latitude, p.longitude))
                    return true
                }

                override fun longPressHelper(p: OsmPoint): Boolean = false
            }),
        )
    }

    lines.filter { it.points.size >= 2 }.forEach { line ->
        overlays.add(
            Polyline().apply {
                setPoints(line.points.map { it.toOsm() })
                outlinePaint.color = line.color.toArgb()
                outlinePaint.strokeWidth = line.widthDp * density
                outlinePaint.isAntiAlias = true
            },
        )
    }

    markers.forEach { m ->
        overlays.add(
            Marker(this).apply {
                position = m.point.toOsm()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = m.title
                setInfoWindow(null)
            },
        )
    }

    overlays.addAll(extraOverlays)
    invalidate()
}

/** Đưa camera bao trọn danh sách điểm (fit bounds). 1 điểm -> zoom vào điểm đó. */
fun MapView.fitToPoints(points: List<GeoPoint>, paddingPx: Int) {
    when {
        points.size >= 2 -> {
            val box = BoundingBox.fromGeoPoints(points.map { it.toOsm() })
            val apply = Runnable { runCatching { zoomToBoundingBox(box, false, paddingPx) } }
            if (width > 0 && height > 0) post(apply)
            else addOnFirstLayoutListener { _, _, _, _, _ -> apply.run() }
        }
        points.size == 1 -> {
            controller.setZoom(16.0)
            controller.setCenter(points.first().toOsm())
        }
    }
}

/**
 * Bản đồ OpenStreetMap dùng chung. Mặc định fit bounds theo mọi điểm của [lines].
 * Truyền [initialCenter] để đặt camera ban đầu thay vì fit (dùng khi màn cho phép chấm điểm).
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    lines: List<MapLine> = emptyList(),
    markers: List<MapMarker> = emptyList(),
    onTap: ((GeoPoint) -> Unit)? = null,
    fitToLines: Boolean = true,
    initialCenter: GeoPoint? = null,
    initialZoom: Double = 15.0,
) {
    val density = LocalDensity.current.density
    val mapView = rememberOsmMapView()
    val latestOnTap by rememberUpdatedState(onTap)
    val tapHandler: ((GeoPoint) -> Unit)? =
        if (onTap != null) { p -> latestOnTap?.invoke(p) } else null

    LaunchedEffect(mapView, initialCenter, initialZoom) {
        initialCenter?.let {
            mapView.controller.setZoom(initialZoom)
            mapView.controller.setCenter(OsmPoint(it.latitude, it.longitude))
        }
    }

    val fitPoints = if (fitToLines) lines.flatMap { it.points } else emptyList()
    LaunchedEffect(mapView, fitPoints) {
        if (fitToLines) mapView.fitToPoints(fitPoints, (24 * density).toInt())
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { it.renderPath(lines, markers, tapHandler, density) },
    )
}

/** Bản đồ tĩnh vẽ một đường (route/segment/trail). Camera fit toàn bộ điểm. */
@Composable
fun PathMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    markEndpoints: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val lines = remember(points, primary) {
        if (points.size >= 2) listOf(MapLine(points, primary, widthDp = 4f)) else emptyList()
    }
    val markers = remember(points, markEndpoints) {
        if (markEndpoints && points.size >= 2) {
            listOf(MapMarker(points.first(), "Bắt đầu"), MapMarker(points.last(), "Kết thúc"))
        } else {
            emptyList()
        }
    }
    OsmMap(modifier = modifier, lines = lines, markers = markers, fitToLines = true)
}
