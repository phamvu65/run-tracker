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

    /** Nét vẽ tay đã làm mượt (đã khép vòng) — để đối chiếu hoặc dùng thẳng làm route. */
    var lastSketch by mutableStateOf<List<GeoPoint>>(emptyList())
        private set

    /** Thông báo một lần cho người dùng (ví dụ: bám đường thất bại). */
    var notice by mutableStateOf<String?>(null)
        private set

    fun toggleDrawMode() {
        drawMode = !drawMode
    }

    fun addPoint(point: GeoPoint) {
        tappedPoints = tappedPoints + point
        planned = null
    }

    /**
     * Nhận nét vẽ tay: rút gọn điểm quá gần nhau, tự khép vòng nếu đầu-cuối gần nhau, rồi
     * TỰ ĐỘNG bám đường luôn (đỡ phải bấm thêm nút). Trong lúc chờ, hiện tạm nét vẽ thô làm
     * preview. Nếu bám đường thất bại/lệch quá (use-case tự loại), rơi về nét vẽ tay — nút
     * "Thử bám đường lại" vẫn còn để người dùng thử lại (VD do mạng chập chờn lúc vừa vẽ).
     */
    fun applySketch(sketch: List<GeoPoint>) {
        val clean = dedupe(sketch, minGapMeters = 6.0)
        if (clean.size < 3) return
        val totalLen = GeoMath.pathDistanceMeters(clean)
        if (totalLen < 150.0) return

        val isLoop = GeoMath.distanceMeters(clean.first(), clean.last()) < maxOf(60.0, totalLen * 0.2)
        val closed = if (isLoop) clean + clean.first() else clean

        lastSketch = closed
        tappedPoints = closed
        notice = null
        planned = freehandRoute(closed)
        snapSketchToRoads()
    }

    /** Bám nét vẽ vào mạng đường. Use-case tự chọn điểm trung gian và loại kết quả lệch. */
    fun snapSketchToRoads() {
        val raw = lastSketch
        if (raw.size < 2 || loading) return
        viewModelScope.launch {
            loading = true
            notice = null
            val result = buildRoute.fromSketch(raw, mode)
            planned = result
            if (!result.snappedToRoads) {
                notice = "Quanh đây không có đường nào bám sát nét vẽ — giữ nguyên nét vẽ tay."
            }
            loading = false
        }
    }

    /** Quay lại dùng nét vẽ tay làm route. */
    fun useSketchAsRoute() {
        val raw = lastSketch
        if (raw.size < 2) return
        notice = null
        planned = freehandRoute(raw)
    }

    fun consumeNotice() {
        notice = null
    }

    private fun freehandRoute(points: List<GeoPoint>) = PlannedRoute(
        polyline = points,
        distanceMeters = GeoMath.pathDistanceMeters(points),
        steps = emptyList(),
        snappedToRoads = false,
    )

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
        lastSketch = emptyList()
        notice = null
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
}
