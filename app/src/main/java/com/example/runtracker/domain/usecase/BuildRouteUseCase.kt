package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.navigation.RouteNavigator
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.domain.tracking.GeoMath
import javax.inject.Inject
import kotlin.math.max

/**
 * Dựng route. Ưu tiên bám đường qua OSRM; nếu lỗi (mất mạng / không khớp) thì
 * fallback nối các điểm bằng đường thẳng.
 */
class BuildRouteUseCase @Inject constructor(
    private val directionsRepository: DirectionsRepository,
) {
    /** Từ các điểm người dùng chấm. */
    suspend operator fun invoke(waypoints: List<GeoPoint>, mode: TravelMode): PlannedRoute {
        if (waypoints.size < 2) {
            return PlannedRoute(waypoints, 0.0, emptyList(), snappedToRoads = false)
        }
        return directionsRepository.route(waypoints, mode).getOrElse { straightLine(waypoints) }
    }

    /**
     * Bám nét vẽ tay vào mạng đường.
     *
     * Nhồi hàng chục điểm trung gian dày đặc vào OSRM là nguyên nhân đường đi "bám rất
     * nhiều đường": mỗi điểm bị ép khớp vào con đường gần nó nhất, hai điểm liền nhau rơi
     * vào hai phố song song là router phải chui vào ngõ rồi vòng ra. Nên ở đây:
     *
     *  1. rút nét vẽ thành ít điểm trung gian, cách nhau khá xa (thử lần lượt [VIA_SPACING_M]);
     *  2. cấm quay đầu tại điểm trung gian;
     *  3. chấm điểm kết quả — dài hơn nét vẽ bao nhiêu, lệch khỏi nét vẽ bao xa;
     *  4. kết quả tệ thì thử lại với ít điểm hơn; vẫn tệ thì giữ nguyên nét vẽ tay
     *     (thà đi đúng hình người dùng vẽ còn hơn một vòng zigzag qua chục con phố).
     */
    suspend fun fromSketch(sketch: List<GeoPoint>, mode: TravelMode): PlannedRoute {
        if (sketch.size < 2) return straightLine(sketch)
        val sketchLength = GeoMath.pathDistanceMeters(sketch)
        if (sketchLength <= 0.0) return straightLine(sketch)

        var best: PlannedRoute? = null
        var bestScore = Double.MAX_VALUE

        for (spacing in VIA_SPACING_M) {
            val count = (sketchLength / spacing).toInt().coerceIn(MIN_VIA_POINTS, MAX_VIA_POINTS)
            val trace = GeoMath.resample(sketch, count)
            if (trace.size < 2) continue

            val candidate = directionsRepository.route(trace, mode, allowUTurns = false).getOrNull()
                ?: continue
            val score = detourScore(candidate, sketch, sketchLength)
            if (score < bestScore) {
                best = candidate
                bestScore = score
            }
            if (score <= GOOD_SCORE) break
        }

        return if (best != null && bestScore <= ACCEPTABLE_SCORE) best else straightLine(sketch)
    }

    /**
     * Điểm phạt của một route đã bám đường so với nét vẽ: 0 là khớp hoàn hảo, càng lớn
     * càng lệch. Gộp hai thành phần — phần đường đi dài hơn nét vẽ, và khoảng lệch xa nhất
     * giữa nét vẽ và đường đi (quy về [DEVIATION_BUDGET_M]).
     */
    private fun detourScore(
        route: PlannedRoute,
        sketch: List<GeoPoint>,
        sketchLength: Double,
    ): Double {
        if (route.polyline.size < 2) return Double.MAX_VALUE
        val lengthPenalty = max(0.0, route.distanceMeters / sketchLength - 1.0)
        val maxDeviation = GeoMath.resample(sketch, DEVIATION_SAMPLES).maxOf { point ->
            RouteNavigator.distanceToPolylineMeters(point, route.polyline)
        }
        return lengthPenalty + maxDeviation / DEVIATION_BUDGET_M
    }

    private fun straightLine(points: List<GeoPoint>) = PlannedRoute(
        polyline = points,
        distanceMeters = GeoMath.pathDistanceMeters(points),
        steps = emptyList(),
        snappedToRoads = false,
    )

    private companion object {
        /** Khoảng cách giữa hai điểm trung gian gửi cho OSRM, thử lần lượt từ dày tới thưa. */
        val VIA_SPACING_M = listOf(250.0, 500.0)
        const val MIN_VIA_POINTS = 3
        const val MAX_VIA_POINTS = 12
        const val DEVIATION_SAMPLES = 24
        const val DEVIATION_BUDGET_M = 80.0

        /** Đủ tốt thì nhận luôn, khỏi gọi mạng thêm lần nữa. */
        const val GOOD_SCORE = 0.5

        /** Tệ hơn mức này thì thà giữ nét vẽ tay. */
        const val ACCEPTABLE_SCORE = 1.2
    }
}
