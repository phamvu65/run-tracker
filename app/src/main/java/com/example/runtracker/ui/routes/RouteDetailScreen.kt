package com.example.runtracker.ui.routes

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.ui.common.PathMap
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint as OsmGeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val route by viewModel.route.collectAsState()
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val downloadState = remember { mutableStateOf<OfflineDownloadState?>(null) }

    LaunchedEffect(viewModel.deleted) {
        if (viewModel.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(route?.name ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            route?.polyline?.let { startOfflineDownload(context, mapView, it, downloadState) }
                        },
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Tải bản đồ offline")
                    }
                    IconButton(onClick = viewModel::delete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Xoá route")
                    }
                },
            )
        },
    ) { padding ->
        val r = route ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (r.polyline.size >= 2) {
                PathMap(
                    points = r.polyline,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    onMapViewReady = { mapView = it },
                )
            }

            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(formatDistanceKm(r.distanceMeters), style = MaterialTheme.typography.titleLarge)

                val steps = r.waypoints.filter { !it.instruction.isNullOrBlank() }
                if (steps.isNotEmpty()) {
                    SectionHeader("Chỉ đường")
                    steps.forEach { wp ->
                        Text("${wp.orderIndex + 1}. ${wp.instruction}", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(
                        "${r.waypoints.size} điểm mốc (không có chỉ đường chi tiết).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    downloadState.value?.let { state ->
        OfflineDownloadDialog(state, onDismiss = { downloadState.value = null })
    }
}

/** Tiến trình tải tile offline quanh 1 route — xem [startOfflineDownload]. */
private data class OfflineDownloadState(
    val progress: Int = 0,
    val total: Int = 0,
    val done: Boolean = false,
    val failed: Boolean = false,
)

/**
 * Tải sẵn tile bản đồ quanh route (nới biên ~15% + tối thiểu ~200m mỗi chiều) ở zoom 14-17 —
 * osmdroid tự cache khi lướt map nhưng không tải trước; mục tiêu ở đây là "offline cho route đã
 * lưu", không phải offline toàn bản đồ. Cần [mapView] thật (lấy qua `PathMap.onMapViewReady`) vì
 * `CacheManager` tải theo đúng tile source đang hiển thị.
 */
private fun startOfflineDownload(
    context: Context,
    mapView: MapView?,
    points: List<GeoPoint>,
    state: MutableState<OfflineDownloadState?>,
) {
    if (mapView == null || points.size < 2) return
    val raw = BoundingBox.fromGeoPoints(ArrayList(points.map { OsmGeoPoint(it.latitude, it.longitude) }))
    val latPad = (raw.latNorth - raw.latSouth) * 0.15 + MIN_PAD_DEGREES
    val lonPad = (raw.lonEast - raw.lonWest) * 0.15 + MIN_PAD_DEGREES
    val bbox = BoundingBox(
        raw.latNorth + latPad,
        raw.lonEast + lonPad,
        raw.latSouth - latPad,
        raw.lonWest - lonPad,
    )

    val cacheManager = CacheManager(mapView)
    state.value = OfflineDownloadState()
    cacheManager.downloadAreaAsync(
        context,
        bbox,
        OFFLINE_MIN_ZOOM,
        OFFLINE_MAX_ZOOM,
        object : CacheManager.CacheManagerCallback {
            override fun onTaskComplete() {
                state.value = state.value?.copy(done = true)
            }

            override fun onTaskFailed(errors: Int) {
                state.value = state.value?.copy(failed = true)
            }

            override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                state.value = state.value?.copy(progress = progress)
            }

            override fun downloadStarted() = Unit

            override fun setPossibleTilesInArea(total: Int) {
                state.value = state.value?.copy(total = total)
            }
        },
    )
}

@Composable
private fun OfflineDownloadDialog(state: OfflineDownloadState, onDismiss: () -> Unit) {
    val finished = state.done || state.failed
    AlertDialog(
        onDismissRequest = { if (finished) onDismiss() },
        confirmButton = {
            if (finished) TextButton(onClick = onDismiss) { Text("Đóng") }
        },
        title = {
            Text(
                when {
                    state.done -> "Đã tải xong"
                    state.failed -> "Tải thất bại"
                    else -> "Đang tải bản đồ offline"
                },
            )
        },
        text = {
            Column {
                when {
                    state.done -> Text("Bản đồ quanh route này đã sẵn sàng dùng khi mất mạng.")
                    state.failed -> Text("Không tải được — kiểm tra kết nối mạng rồi thử lại.")
                    else -> {
                        val fraction = if (state.total > 0) state.progress / state.total.toFloat() else 0f
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        Text("${state.progress}/${state.total} tile")
                    }
                }
            }
        },
    )
}

private const val OFFLINE_MIN_ZOOM = 14
private const val OFFLINE_MAX_ZOOM = 17
private const val MIN_PAD_DEGREES = 0.002
