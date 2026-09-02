package com.example.runtracker.ui.routes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.tracking.GeoMath
import com.example.runtracker.domain.usecase.BuildRouteUseCase
import com.example.runtracker.domain.usecase.SaveRouteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteBuilderViewModel @Inject constructor(
    private val buildRoute: BuildRouteUseCase,
    private val saveRoute: SaveRouteUseCase,
) : ViewModel() {

    var tappedPoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set
    var mode by mutableStateOf(TravelMode.WALKING)
        private set
    var planned by mutableStateOf<PlannedRoute?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var savedRouteId by mutableStateOf<String?>(null)
        private set

    /** true = vẽ tay một vòng; false = chạm từng điểm. */
    var drawMode by mutableStateOf(false)
        private set

    fun toggleDrawMode() {
        drawMode = !drawMode
    }

    fun addPoint(point: GeoPoint) {
        tappedPoints = tappedPoints + point
        planned = null
    }

    /**
     * Nhận nét vẽ tay: rút gọn thành ~14 điểm cách đều, tự khép vòng nếu đầu-cuối gần nhau,
     * rồi bám đường luôn. Người dùng chỉ cần khoanh quanh khu vực muốn chạy.
     */
    fun applySketch(sketch: List<GeoPoint>) {
        val clean = dedupe(sketch, minGapMeters = 12.0)
        if (clean.size < 3) return
        val totalLen = GeoMath.pathDistanceMeters(clean)
        if (totalLen < 150.0) return

        var pts = GeoMath.resample(clean, WAYPOINTS)
        val gap = GeoMath.distanceMeters(pts.first(), pts.last())
        if (gap < maxOf(60.0, totalLen * 0.2)) {
            pts = pts.dropLast(1) + pts.first() // khép vòng chính xác
        }
        tappedPoints = pts
        planned = null
        computeRoute()
    }

    private fun dedupe(points: List<GeoPoint>, minGapMeters: Double): List<GeoPoint> {
        if (points.isEmpty()) return points
        val out = mutableListOf(points.first())
        for (p in points.drop(1)) {
            if (GeoMath.distanceMeters(out.last(), p) >= minGapMeters) out += p
        }
        return out
    }

    fun undo() {
        if (tappedPoints.isNotEmpty()) {
            tappedPoints = tappedPoints.dropLast(1)
            planned = null
        }
    }

    fun clear() {
        tappedPoints = emptyList()
        planned = null
    }

    fun selectMode(newMode: TravelMode) {
        if (newMode != mode) {
            mode = newMode
            planned = null
        }
    }

    fun computeRoute() {
        if (tappedPoints.size < 2 || loading) return
        viewModelScope.launch {
            loading = true
            planned = buildRoute(tappedPoints, mode)
            loading = false
        }
    }

    fun save(name: String) {
        val current = planned ?: return
        viewModelScope.launch {
            savedRouteId = saveRoute(name, current, tappedPoints)
        }
    }

    fun consumeSaved() {
        savedRouteId = null
    }

    private companion object {
        const val WAYPOINTS = 14
    }
}
