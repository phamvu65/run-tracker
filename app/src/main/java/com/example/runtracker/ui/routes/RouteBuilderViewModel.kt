package com.example.runtracker.ui.routes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.repository.RouteRepository
import com.example.runtracker.domain.tracking.GeoMath
import com.example.runtracker.domain.usecase.BuildRouteUseCase
import com.example.runtracker.domain.usecase.SaveRouteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import com.example.runtracker.di.DefaultDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class RouteBuilderViewModel @Inject constructor(
    private val buildRoute: BuildRouteUseCase,
    private val saveRoute: SaveRouteUseCase,
    private val routeRepository: RouteRepository,
    savedStateHandle: SavedStateHandle,
    @DefaultDispatcher private val routingDispatcher: CoroutineDispatcher,
) : ViewModel() {
    var tappedPoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set
    var mode by mutableStateOf(TravelMode.WALKING)
        private set
    var planned by mutableStateOf<PlannedRoute?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var opening by mutableStateOf(false)
        private set
    var savedRouteId by mutableStateOf<String?>(null)
        private set
    var routeName by mutableStateOf("")
        private set
    var openedRouteCenter by mutableStateOf<GeoPoint?>(null)
        private set
    var drawMode by mutableStateOf(false)
        private set
    var lastSketch by mutableStateOf<List<GeoPoint>>(emptyList())
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var selectedPointIndex by mutableStateOf<Int?>(null)
        private set

    private var revision = 0L
    private var buildJob: Job? = null
    private var existing: Route? = null
    private val editable get() = !saving && !opening && savedRouteId == null
    val canSave get() = editable && !loading && planned?.snappedToRoads == true

    init {
        val routeId: String? = savedStateHandle["editRouteId"]
        if (routeId != null) {
            opening = true
            viewModelScope.launch {
                try {
                    val route = routeRepository.getRoute(routeId)
                    if (route == null) {
                        notice = "Không tìm thấy lộ trình."
                    } else {
                        existing = route
                        routeName = route.name
                        openedRouteCenter = route.polyline.firstOrNull()
                        mode = route.travelMode ?: TravelMode.WALKING
                        drawMode = route.drawnFromSketch || route.sourcePoints.isEmpty()
                        tappedPoints = route.sourcePoints.ifEmpty { route.polyline }
                        lastSketch = if (drawMode) tappedPoints else emptyList()
                        // Rebuild before saving: legacy routes may contain straight-line gaps.
                        notice = "Kiểm tra chế độ di chuyển và tính đường lại trước khi lưu."
                    }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { notice = "Không mở được lộ trình. Hãy thử lại."
                } finally { opening = false }
            }
        }
    }

    private fun invalidate() {
        revision++
        buildJob?.cancel()
        buildJob = null
        loading = false
        planned = null
        notice = null
    }

    fun toggleDrawMode() {
        if (!editable) return
        invalidate()
        drawMode = !drawMode
        tappedPoints = emptyList()
        lastSketch = emptyList()
        selectedPointIndex = null
    }

    fun addPoint(point: GeoPoint) {
        if (!editable || drawMode) return
        invalidate()
        val selected = selectedPointIndex
        if (selected != null && selected in tappedPoints.indices) {
            tappedPoints = tappedPoints.toMutableList().apply { set(selected, point) }
        } else {
            tappedPoints = tappedPoints + point
        }
        selectedPointIndex = null
    }

    fun selectPoint(index: Int) {
        if (!editable) return
        selectedPointIndex = if (selectedPointIndex == index) null else index.takeIf { it in tappedPoints.indices }
    }

    fun clearSelection() { selectedPointIndex = null }

    fun deleteSelectedPoint() {
        if (!editable) return
        val index = selectedPointIndex ?: return
        if (index !in tappedPoints.indices) return
        invalidate()
        tappedPoints = tappedPoints.toMutableList().apply { removeAt(index) }
        selectedPointIndex = null
    }

    fun applySketch(sketch: List<GeoPoint>) {
        if (!editable) return
        val clean = mutableListOf<GeoPoint>()
        for (point in sketch) {
            if (clean.isEmpty() || GeoMath.distanceMeters(clean.last(), point) >= 4.0) clean += point
        }
        // Preserve the drawn endpoint; never close a gap by a chord across a lake.
        sketch.lastOrNull()?.let { if (clean.lastOrNull() != it) clean += it }
        if (clean.size < 2 || GeoMath.pathDistanceMeters(clean) < 20.0) return
        invalidate()
        drawMode = true
        selectedPointIndex = null
        lastSketch = clean
        tappedPoints = clean
        snapSketchToRoads()
    }

    fun snapSketchToRoads() {
        if (!editable || lastSketch.size < 2) return
        compute(lastSketch, sketch = true)
    }

    fun undo() {
        if (!editable || tappedPoints.isEmpty()) return
        invalidate()
        tappedPoints = tappedPoints.dropLast(1)
        selectedPointIndex = null
    }

    fun clear() {
        if (!editable) return
        invalidate()
        tappedPoints = emptyList()
        lastSketch = emptyList()
        selectedPointIndex = null
    }

    fun selectMode(newMode: TravelMode) {
        if (!editable || newMode == mode) return
        invalidate()
        mode = newMode
    }

    fun computeRoute() {
        if (!editable || tappedPoints.size < 2) return
        compute(tappedPoints, sketch = false)
    }

    private fun compute(points: List<GeoPoint>, sketch: Boolean) {
        invalidate()
        val requestRevision = revision
        val input = points.toList()
        val travelMode = mode
        loading = true
        buildJob = viewModelScope.launch {
            try {
                val result = withTimeoutOrNull(35_000) {
                    withContext(routingDispatcher) {
                        if (sketch) buildRoute.fromSketch(input, travelMode) else buildRoute(input, travelMode)
                    }
                }
                if (revision != requestRevision) return@launch
                planned = result?.takeIf { it.snappedToRoads }
                if (planned == null) notice =
                    "Chưa tìm được đường phù hợp. Kiểm tra mạng, chỉnh nét vẽ theo lối đi trên bản đồ hoặc thêm điểm ở chỗ rẽ. Bản nháp chưa thể lưu."
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                if (revision == requestRevision) notice = "Không tính được đường. Hãy thử lại."
            } finally {
                if (revision == requestRevision) loading = false
            }
        }
    }

    fun save(name: String) {
        if (!canSave || name.isBlank()) return
        val current = planned ?: return
        val input = tappedPoints.toList()
        val travelMode = mode
        val sketch = drawMode
        saving = true
        viewModelScope.launch {
            try {
                savedRouteId = saveRoute(name, current, input, travelMode, sketch, existing)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { notice = "Không lưu được lộ trình. Hãy thử lại."
            } finally { saving = false }
        }
    }

    fun consumeSaved() { savedRouteId = null }
}
