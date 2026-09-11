package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.navigation.RouteNavigator
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.domain.tracking.GeoMath
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

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
     *  3. mỗi điểm chỉ được khớp vào đường có hướng gần với hướng nét vẽ tại chỗ đó
     *     ([BEARING_RANGE_DEGREES]) — bán kính + cấm quay đầu tại via (bước 4) không ngăn
     *     được việc via khớp vào miệng một con hẻm gần đó rồi phải đi vào-quay-ra (quay đầu
     *     chỉ bị cấm NGAY TẠI via, không cấm việc vòng vào một ngõ cụt cạnh via); hẻm hầu hết
     *     đâm vuông góc vào phố chính nên ràng buộc hướng loại được phần lớn kiểu "vòng vào
     *     rồi vòng ra" này (đã kiểm chứng trực tiếp với routing.openstreetmap.de);
     *  4. cấm quay đầu tại điểm trung gian;
     *  5. mỗi CHẶNG (giữa 2 via point liên tiếp) mà router phải đi vòng quá xa so với khoảng
     *     cách thẳng giữa 2 via point đó ([LEG_DETOUR_RATIO]) thì THAY chặng đó bằng đoạn
     *     thẳng nối 2 via point ([fillUnmappedGaps]) — trường hợp thực tế gặp phải: lối đi
     *     ven hồ chỉ được vẽ trong OSM một đoạn, hết đoạn đó router buộc phải vòng qua cả một
     *     khu dân cư kế bên để nối tiếp vì đó là đường LIỀN MẠCH duy nhất có trong dữ liệu,
     *     dù không phải điều người vẽ tay muốn. Không có mật độ via/bán kính/hướng nào sửa
     *     được việc này vì đây là lỗ hổng DỮ LIỆU OSM (đoạn đường ven hồ chưa được vẽ), không
     *     phải lỗi khớp điểm — nên vá trực tiếp bằng đoạn thẳng theo đúng nét vẽ tay ở đúng
     *     chặng đó, giữ nguyên phần còn lại của route đã bám đường tốt;
     *  6. chấm điểm các kết quả tìm được (sau khi đã vá ở bước 5) — dài hơn nét vẽ bao nhiêu,
     *     lệch khỏi nét vẽ bao xa — CHỈ để CHỌN cái tốt nhất trong số đó, KHÔNG dùng để từ
     *     chối. Một route đã bám đường, dù lệch nét vẽ, vẫn luôn trực quan hơn nét vẽ tay thô
     *     cắt ngang nhà cửa/hồ, nên hễ có ít nhất một kết quả bám đường được là dùng luôn. Chỉ
     *     rơi về nét vẽ tay khi KHÔNG có kết quả nào cả (mất mạng, hoặc không đường nào trong
     *     bán kính/hướng ở mọi mật độ điểm đã thử).
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
                bearingsDegrees = localBearings(trace),
                bearingRangeDegrees = BEARING_RANGE_DEGREES,
            ).getOrNull()?.let { fillUnmappedGaps(it, trace) } ?: continue
            val score = detourScore(candidate, sketch, sketchLength)
            if (score < bestScore) {
                best = candidate
                bestScore = score
            }
            if (score <= GOOD_SCORE) break
        }

        return best ?: straightLine(sketch)
    }

    /**
     * Thay các chặng bị router đi vòng quá xa (so với đường thẳng giữa 2 via point của chặng
     * đó) bằng chính đoạn thẳng đó — xem giải thích ở bước 5 trong doc của [fromSketch].
     * Bỏ qua (trả nguyên [route]) nếu số chặng không khớp số via point (an toàn nếu OSRM trả
     * về khác định dạng mong đợi).
     */
    private fun fillUnmappedGaps(route: PlannedRoute, trace: List<GeoPoint>): PlannedRoute {
        if (route.legs.size != trace.size - 1) return route

        val polyline = ArrayList<GeoPoint>(route.polyline.size)
        var totalDistance = 0.0
        for (i in route.legs.indices) {
            val leg = route.legs[i]
            val chordLength = GeoMath.distanceMeters(trace[i], trace[i + 1])
            val legPoints: List<GeoPoint>
            val legDistance: Double
            if (chordLength > 0.0 && leg.distanceMeters > chordLength * LEG_DETOUR_RATIO) {
                legPoints = listOf(trace[i], trace[i + 1])
                legDistance = chordLength
            } else {
                legPoints = leg.polyline
                legDistance = leg.distanceMeters
            }
            for (p in legPoints) {
                if (polyline.isEmpty() || polyline.last() != p) polyline += p
            }
            totalDistance += legDistance
        }
        if (polyline.size < 2) return route
        return route.copy(polyline = polyline, distanceMeters = totalDistance)
    }

    /** Hướng nét vẽ tại mỗi điểm — trung bình từ điểm trước tới điểm sau (điểm đầu/cuối dùng
     *  hướng với hàng xóm duy nhất). */
    private fun localBearings(trace: List<GeoPoint>): List<Double> {
        val n = trace.size
        return List(n) { i ->
            val prev = trace[max(0, i - 1)]
            val next = trace[min(n - 1, i + 1)]
            GeoMath.bearingDegrees(prev, next)
        }
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

        /** Mỗi điểm trung gian chỉ được khớp vào đường có hướng lệch không quá mức này so
         *  với hướng nét vẽ tại chỗ đó — chặn việc vòng vào một con hẻm cắt ngang rồi quay ra.
         *  60° đã kiểm chứng vẫn chặn được kiểu lệch đó (kiểm thử trực tiếp với
         *  routing.openstreetmap.de) trong khi ít làm rớt cả yêu cầu route (NoRoute) hơn mức
         *  hẹp hơn ở những khúc cua/ngã ba không thẳng hàng tuyệt đối với nét vẽ. */
        const val BEARING_RANGE_DEGREES = 60.0

        /** Đủ tốt thì nhận luôn, khỏi gọi mạng thêm lần nữa. */
        const val GOOD_SCORE = 0.5

        /** Một chặng (giữa 2 via point liên tiếp) dài hơn đường thẳng nối chúng quá mức này
         *  thì coi là router phải vòng qua khu vực khác do mạng đường OSM thiếu đoạn nối
         *  đúng lúc đó — thay bằng đoạn thẳng thay vì giữ nguyên đường vòng. */
        const val LEG_DETOUR_RATIO = 1.8
    }
}
