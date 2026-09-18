package com.example.runtracker.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.domain.model.GeoPoint
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.coroutines.resume
import org.osmdroid.util.GeoPoint as OsmPoint

/**
 * Một đường vẽ trên bản đồ. `widthDp` là bề rộng nét theo dp (nhân với mật độ khi vẽ).
 * [showDirection] = true: rải mũi tên chỉ chiều đi dọc đường.
 * [dashed] = true: vẽ nét đứt thay vì nét liền — dùng cho đoạn ước tính theo nét vẽ tay vì
 * chưa có dữ liệu đường thật (xem `PlannedRoute.gapPolylines`), để không bị hiểu nhầm là bám
 * sai hay app bị lỗi.
 */
data class MapLine(
    val points: List<GeoPoint>,
    val color: Color,
    val widthDp: Float = 4f,
    val showDirection: Boolean = false,
    val dashed: Boolean = false,
)

/**
 * Màu đường dẫn phụ/mờ trên bản đồ (nét vẽ tay đối chiếu, route chưa chọn...) — CỐ Ý không
 * lấy `colorScheme.outline`: token đó dành cho viền UI, ở theme tối gần như đen nên đè lên
 * ảnh bản đồ (thường sáng) trông như một nét đen chứ không phải đường mờ để đối chiếu.
 */
val MapGuideLineColor = Color(0xFF9AA0A6).copy(alpha = 0.55f)

/** Một điểm mốc trên bản đồ. [MarkerStyle.BADGE] dùng [color] + [label] làm chấm tròn có chữ. */
data class MapMarker(
    val point: GeoPoint,
    val title: String? = null,
    val style: MarkerStyle = MarkerStyle.PIN,
    val color: Color = Color.Unspecified,
    val label: String? = null,
)

private fun GeoPoint.toOsm() = OsmPoint(latitude, longitude)

/** Kiểu hiển thị bản đồ: đường phố (OSM) hoặc ảnh vệ tinh (Esri). */
enum class MapStyle { STREET, SATELLITE }

/**
 * Nguồn tile đường phố mặc định. KHÔNG dùng `tile.openstreetmap.org` — nhiều ISP ở VN
 * (FPT...) đầu độc DNS domain `openstreetmap.org` về 127.0.0.1 nên tile không tải được
 * (bản đồ chỉ hiện lưới ô). `tile.openstreetmap.de` là mirror MAPNIK, không bị chặn.
 */
val DefaultTileSource: ITileSource = XYTileSource(
    "OpenStreetMap.de",
    0, 19, 256, ".png",
    arrayOf("https://tile.openstreetmap.de/"),
    "© OpenStreetMap contributors",
)

/**
 * Ảnh vệ tinh Esri World Imagery — miễn phí, không cần key, không bị chặn DNS ở VN.
 * URL theo thứ tự z/y/x (khác chuẩn z/x/y của [XYTileSource]) nên phải override.
 */
private val SatelliteTileSource: ITileSource = object : OnlineTileSourceBase(
    "Esri.WorldImagery",
    0, 19, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Nguồn: Esri, Maxar, Earthstar Geographics",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex)
}

fun tileSourceFor(style: MapStyle): ITileSource =
    if (style == MapStyle.SATELLITE) SatelliteTileSource else DefaultTileSource

/**
 * `MapView` osmdroid gắn với vòng đời Compose: onResume/onPause theo lifecycle,
 * onDetach khi rời khỏi composition.
 */
@Composable
fun rememberOsmMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(DefaultTileSource)
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
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                outlinePaint.isAntiAlias = true
                if (line.dashed) {
                    outlinePaint.pathEffect = DashPathEffect(floatArrayOf(18f * density, 14f * density), 0f)
                }
                if (line.showDirection) {
                    setMilestoneManagers(directionMilestones(line.color, density))
                }
            },
        )
    }

    markers.forEach { m ->
        overlays.add(
            Marker(this).apply {
                position = m.point.toOsm()
                when (m.style) {
                    MarkerStyle.BADGE -> {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = badgeDrawable(context, m.color, m.label, density)
                    }
                    MarkerStyle.EMOJI -> {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = emojiDrawable(context, m.label.orEmpty(), density)
                    }
                    MarkerStyle.PIN -> setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
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

/** Vị trí GPS gần nhất qua FusedLocation (null nếu chưa có quyền / chưa có fix). */
@SuppressLint("MissingPermission")
suspend fun lastKnownLocation(context: Context): GeoPoint? {
    if (!context.hasLocationPermission()) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc?.let { GeoPoint(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
    }
}

/**
 * Cụm nút nổi góc phải bản đồ: đổi kiểu (đường phố / vệ tinh) và về vị trí của tôi.
 * [onRecenter] null -> ẩn nút định vị.
 */
@Composable
fun MapControls(
    style: MapStyle,
    onToggleStyle: () -> Unit,
    onRecenter: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MapControlButton(onClick = onToggleStyle) {
            Text(
                if (style == MapStyle.STREET) "🛰" else "🗺",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (onRecenter != null) {
            MapControlButton(onClick = onRecenter) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "Về vị trí của tôi",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Nút tròn nổi trên bản đồ kiểu GoRun "Map Guide": nền tối trong suốt + viền mảnh, thay vì
 * thẻ đặc kiểu Material — đọc được trên mọi nền ảnh bản đồ mà không che khuất quá nhiều.
 */
@Composable
private fun MapControlButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 2.dp,
        modifier = Modifier.size(46.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Bản đồ OpenStreetMap dùng chung. Mặc định fit bounds theo mọi điểm của [lines].
 * Truyền [initialCenter] để đặt camera ban đầu thay vì fit (dùng khi màn cho phép chấm điểm).
 * [showMyLocation] = true: hiện chấm vị trí + nút "về vị trí của tôi" (cần quyền vị trí).
 * [controlsPadding]: chừa lề cho cụm nút khi bản đồ bị panel khác che một phần.
 * [drawMode] = true: khoá pan/zoom, cho vẽ tay một đường; thả tay -> [onSketch] nhận
 * danh sách điểm địa lý của nét vẽ (dùng cho dựng route "vẽ vòng").
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
    showMyLocation: Boolean = false,
    showControls: Boolean = true,
    controlsPadding: PaddingValues = PaddingValues(12.dp),
    drawMode: Boolean = false,
    onSketch: ((List<GeoPoint>) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val mapView = rememberOsmMapView()
    val latestOnTap by rememberUpdatedState(onTap)
    val tapHandler: ((GeoPoint) -> Unit)? =
        if (onTap != null) { p -> latestOnTap?.invoke(p) } else null

    var style by rememberSaveable { mutableStateOf(MapStyle.STREET) }
    LaunchedEffect(mapView, style) { mapView.setTileSource(tileSourceFor(style)) }

    // Không remember: đọc lại mỗi lần recompose để bắt được lúc người dùng vừa cấp quyền.
    val hasLocationPermission = context.hasLocationPermission()
    val myLocationOverlay = remember(mapView, showMyLocation, hasLocationPermission) {
        if (showMyLocation && hasLocationPermission) {
            MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { disableFollowLocation() }
        } else {
            null
        }
    }
    DisposableEffect(myLocationOverlay) {
        myLocationOverlay?.enableMyLocation()
        onDispose { myLocationOverlay?.disableMyLocation() }
    }

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

    val recenter: (() -> Unit)? = when {
        showMyLocation && hasLocationPermission -> {
            {
                val fix = myLocationOverlay?.myLocation
                if (fix != null) {
                    mapView.controller.animateTo(fix)
                    mapView.controller.setZoom(16.0)
                } else {
                    scope.launch {
                        lastKnownLocation(context)?.let {
                            mapView.controller.animateTo(OsmPoint(it.latitude, it.longitude))
                            mapView.controller.setZoom(16.0)
                        }
                    }
                }
            }
        }
        fitToLines && fitPoints.size >= 2 -> {
            { mapView.fitToPoints(fitPoints, (24 * density).toInt()) }
        }
        else -> null
    }

    val latestOnSketch by rememberUpdatedState(onSketch)
    var sketchPx by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val sketchColor = MaterialTheme.colorScheme.primary

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = {
                it.renderPath(
                    lines, markers, tapHandler, density,
                    extraOverlays = listOfNotNull(myLocationOverlay),
                )
            },
        )

        if (drawMode) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(drawMode) {
                        detectDragGestures(
                            onDragStart = { sketchPx = listOf(it) },
                            onDrag = { change, _ ->
                                change.consume()
                                sketchPx = sketchPx + change.position
                            },
                            onDragEnd = {
                                val proj = mapView.projection
                                val geo = sketchPx.map {
                                    val p = proj.fromPixels(it.x.toInt(), it.y.toInt())
                                    GeoPoint(p.latitude, p.longitude)
                                }
                                sketchPx = emptyList()
                                if (geo.size >= 3) latestOnSketch?.invoke(geo)
                            },
                            onDragCancel = { sketchPx = emptyList() },
                        )
                    },
            ) {
                if (sketchPx.size >= 2) {
                    val path = Path().apply {
                        moveTo(sketchPx.first().x, sketchPx.first().y)
                        sketchPx.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path,
                        color = sketchColor,
                        style = Stroke(width = 5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }

        if (showControls) {
            MapControls(
                style = style,
                onToggleStyle = { style = if (style == MapStyle.STREET) MapStyle.SATELLITE else MapStyle.STREET },
                onRecenter = recenter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(controlsPadding),
            )
        }
    }
}

/**
 * Bản đồ tĩnh vẽ một đường (route/segment/trail). Camera fit toàn bộ điểm.
 * Mặc định có mũi tên chỉ chiều đi + mốc xuất phát / về đích.
 */
@Composable
fun PathMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    markEndpoints: Boolean = true,
    showMyLocation: Boolean = false,
    showDirection: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val lines = remember(points, primary, showDirection) {
        if (points.size >= 2) {
            listOf(MapLine(points, primary, widthDp = 4.5f, showDirection = showDirection))
        } else {
            emptyList()
        }
    }
    val markers = remember(points, markEndpoints) {
        if (markEndpoints) startFinishMarkers(points) else emptyList()
    }
    OsmMap(
        modifier = modifier,
        lines = lines,
        markers = markers,
        fitToLines = true,
        showMyLocation = showMyLocation,
    )
}
