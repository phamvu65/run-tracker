package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode

interface DirectionsRepository {

    /**
     * Bám đường qua một chuỗi điểm (điểm người dùng chấm, hoặc điểm rút từ nét vẽ tay).
     * Thất bại (mất mạng...) -> [Result.failure].
     *
     * [allowUTurns] = false: cấm quay đầu tại các điểm trung gian. Cần cho nét vẽ tay —
     * hai điểm liền nhau có thể bám vào hai phố song song, cho phép quay đầu thì router
     * sẽ chui vào ngõ rồi vòng ra, ra đường đi zigzag qua rất nhiều phố.
     */
    suspend fun route(
        waypoints: List<GeoPoint>,
        mode: TravelMode,
        allowUTurns: Boolean = true,
    ): Result<PlannedRoute>
}
