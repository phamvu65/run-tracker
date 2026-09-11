package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode

interface DirectionsRepository {

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
     */
    suspend fun route(
        waypoints: List<GeoPoint>,
        mode: TravelMode,
        allowUTurns: Boolean = true,
        radiusMeters: Double? = null,
    ): Result<PlannedRoute>
}
