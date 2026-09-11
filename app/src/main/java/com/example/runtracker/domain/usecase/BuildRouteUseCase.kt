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
     * Hai lỗi đối nghịch cần tránh:
     *  - điểm trung gian quá DÀY (mỗi ~40m): điểm bị ép khớp vào đường gần nó nhất, hai điểm
     *    liền nhau rơi vào hai phố song song là router chui vào ngõ rồi vòng ra;
     *  - điểm trung gian quá THƯA (vài trăm mét): router được tự do chọn "đường nhanh nhất"
     *    giữa hai điểm xa nhau, dễ tạt qua phố khác hẳn nét vẽ dù điểm đầu/cuối vẫn khớp.
     *
     * Cách xử lý ở đây:
     *  1. rút nét vẽ thành các điểm trung gian cách đều, thử lần lượt từ dày tới thưa
     *     ([VIA_SPACING_M]) — dày trước để bám sát hình vẽ, thưa hơn nếu dày thất bại
     *     (VD cắt qua công viên/khu không có đường);
     *  2. mỗi điểm chỉ được khớp vào đường trong bán kính [SNAP_RADIUS_METERS] — đủ rộng để
     *     chịu sai số tay vẽ và vẫn tìm được đường ở những chỗ thưa (quảng trường, ven sông),
     *     đủ hẹp để KHÔNG nhảy sang một phố song song ở xa hơn;
     *  3. cấm quay đầu tại điểm trung gian;
     *  4. chấm điểm kết quả — dài hơn nét vẽ bao nhiêu, lệch khỏi nét vẽ bao xa;
     *  5. NGƯỠNG CHẤP NHẬN CỐ Ý RỘNG RÃI ([ACCEPTABLE_SCORE]) — thà lấy một route đã bám
     *     đường dù hơi lệch (dữ liệu OSM khu vực có thể thiếu ngõ nhỏ khiến bám lệch đôi chút)
     *     còn hơn trả về nét vẽ tay thô cắt ngang nhà cửa. Chỉ khi KHÔNG có kết quả nào tìm
     *     được (mất mạng, hoặc không đường nào trong bán kính) mới rơi về nét vẽ tay.
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

            val candidate = directionsRepository.route(
                waypoints = trace,
                mode = mode,
                allowUTurns = false,
                radiusMeters = SNAP_RADIUS_METERS,
            ).getOrNull() ?: continue
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
        val VIA_SPACING_M = listOf(80.0, 150.0, 300.0)
        const val MIN_VIA_POINTS = 3
        const val MAX_VIA_POINTS = 40
        const val DEVIATION_SAMPLES = 24
        const val DEVIATION_BUDGET_M = 100.0

        /** Mỗi điểm trung gian chỉ được khớp vào đường trong bán kính này — chặn việc nhảy
         *  sang một phố song song ở xa hơn nét vẽ. */
        const val SNAP_RADIUS_METERS = 60.0

        /** Đủ tốt thì nhận luôn, khỏi gọi mạng thêm lần nữa. */
        const val GOOD_SCORE = 0.5

        /**
         * Tệ hơn mức này thì thà giữ nét vẽ tay. Cố ý rộng rãi: chấp nhận route dài hơn nét vẽ
         * tới ~60% NẾU bám khá sát (lệch tối đa nhỏ), hoặc lệch tối đa gần bằng
         * [DEVIATION_BUDGET_M] NẾU quãng đường gần đúng — chỉ loại khi cả hai cùng tệ.
         */
        const val ACCEPTABLE_SCORE = 1.6
    }
}
