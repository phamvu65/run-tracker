package com.example.runtracker.domain.usecase

import android.util.Log
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ElevationRepository
import com.example.runtracker.domain.tracking.ElevationCorrection
import com.example.runtracker.domain.tracking.RouteStats
import javax.inject.Inject

private const val TAG = "CorrectElevation"

/**
 * Hiệu chỉnh lại altitude của trace bằng dữ liệu địa hình thật (DEM, qua [ElevationRepository])
 * khi buổi tập không có đầy đủ mẫu khí áp đã hiệu chỉnh và còn mới.
 * Có phần cứng barometer không đồng nghĩa cảm biến đã cung cấp dữ liệu hợp lệ.
 * Buổi khôi phục chưa biết nguồn độ cao nên cũng thử DEM.
 *
 * Không ném lỗi — mất mạng / API lỗi thì giữ nguyên GPS altitude thô, không chặn việc chốt buổi tập.
 */
class CorrectElevationUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val elevationRepository: ElevationRepository,
) {
    suspend operator fun invoke(
        activityId: String,
        points: List<RoutePoint>,
        reliableBarometricAltitude: Boolean = false,
    ): List<RoutePoint> {
        if (reliableBarometricAltitude || points.size < 2) return points

        val distances = RouteStats.cumulativeDistances(points)
        val sampleIndices = ElevationCorrection.sampleIndices(distances)
        val sampleGeoPoints = sampleIndices.map { GeoPoint(points[it].latitude, points[it].longitude) }
        val sampleElevations = elevationRepository.elevationsFor(sampleGeoPoints)
            .onFailure { Log.w(TAG, "elevation lookup failed for activity $activityId", it) }
            .getOrNull() ?: return points

        if (sampleElevations.size != sampleIndices.size || sampleElevations.any { !it.isFinite() }) return points
        val corrected = ElevationCorrection.interpolate(distances, sampleIndices, sampleElevations)
        val result = points.mapIndexed { i, point -> point.copy(altitude = corrected[i]) }
        activityRepository.updateRoutePoints(activityId, result)
        return result
    }
}
