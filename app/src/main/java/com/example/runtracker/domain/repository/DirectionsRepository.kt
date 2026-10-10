package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode

interface DirectionsRepository {

    /** Match a noisy sketch as an ordered, connected trace; never join separate matches. */
    suspend fun matchSketch(points: List<GeoPoint>, mode: TravelMode, radiusMeters: Double): Result<PlannedRoute> =
        Result.failure(UnsupportedOperationException("Trace matching unavailable"))

    /**
     * Bám đường qua một chuỗi điểm (điểm người dùng chấm, hoặc điểm rút từ nét vẽ tay).
     * Thất bại (mất mạng, không tìm được đường trong bán kính...) -> [Result.failure].
     *
     * [allowUTurns] = false: cấm quay đầu tại các điểm trung gian. Cần cho nét vẽ tay —
     * hai điểm liền nhau có thể bám vào hai phố song song, cho phép quay đầu thì router
     * sẽ chui vào ngõ rồi vòng ra, ra đường đi zigzag qua rất nhiều phố.
     *
     * [radiusMeters] != null: mỗi điểm chỉ được khớp vào đường trong bán kính này (mét).
     * Dùng cho nét vẽ tay để chặn việc một điểm nhảy sang khớp vào một phố song song ở xa
     * hơn thay vì phố gần nét vẽ nhất. null = không giới hạn (mặc định của OSRM) — dùng cho
     * điểm người dùng tự chấm, vì điểm đó có thể cố ý đặt xa đường (VD cổng công viên).
     *
     * [bearingsDegrees] != null: mỗi điểm (0-360, cùng thứ tự [waypoints]) chỉ được khớp vào
     * đường có hướng lệch không quá [bearingRangeDegrees] so với hướng này. Đây là lý do chính
     * khiến bám nét vẽ tay hay "chui vào ngõ rồi vòng ra": bán kính + cấm quay đầu tại via
     * không ngăn được việc điểm khớp vào một con hẻm gần đó rồi phải quay lại (vì quay đầu
     * chỉ bị cấm ngay tại via, không cấm đi vào rồi ra một ngõ cụt gần via) — hẻm thường đâm
     * vuông góc vào phố chính nên ràng buộc hướng đi loại được phần lớn trường hợp này.
     * null = không giới hạn hướng — dùng cho điểm người dùng tự chấm.
     */
    suspend fun route(
        waypoints: List<GeoPoint>,
        mode: TravelMode,
        allowUTurns: Boolean = true,
        radiusMeters: Double? = null,
        bearingsDegrees: List<Double>? = null,
        bearingRangeDegrees: Double = 45.0,
    ): Result<PlannedRoute>
}
