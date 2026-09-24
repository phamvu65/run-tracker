package com.example.runtracker.domain.usecase

import android.util.Log
import com.example.runtracker.domain.health.BarometerSource
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ElevationRepository
import com.example.runtracker.domain.tracking.ElevationCorrection
import javax.inject.Inject

private const val TAG = "CorrectElevation"

/**
 * Hiệu chỉnh lại altitude của trace bằng dữ liệu địa hình thật (DEM, qua [ElevationRepository])
 * khi máy KHÔNG có barometer — lúc đó `RoutePoint.altitude` đang là GPS altitude thô, sai số
 * thường ±10-30m và rất nhiễu (xem `LocationTrackingService.toRoutePoint`). Máy có barometer thì
 * dữ liệu đã đủ tốt (mượt hơn, không lệ thuộc mạng) nên bỏ qua, không gọi API vô ích.
 *
 * Không ném lỗi — mất mạng / API lỗi thì giữ nguyên GPS altitude thô, không chặn việc chốt buổi tập.
 */
class CorrectElevationUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val elevationRepository: ElevationRepository,
    private val barometerSource: BarometerSource,
) {
    suspend operator fun invoke(activityId: String, points: List<RoutePoint>): List<RoutePoint> {
        if (barometerSource.isSupported() || points.size < 2) return points

        val sampleIndices = ElevationCorrection.sampleIndices(points.size)
        val sampleGeoPoints = sampleIndices.map { GeoPoint(points[it].latitude, points[it].longitude) }
        val sampleElevations = elevationRepository.elevationsFor(sampleGeoPoints)
            .onFailure { Log.w(TAG, "elevation lookup failed for activity $activityId", it) }
            .getOrNull() ?: return points

        val corrected = ElevationCorrection.interpolate(points.size, sampleIndices, sampleElevations)
        val result = points.mapIndexed { i, point -> point.copy(altitude = corrected[i]) }
        activityRepository.updateRoutePoints(activityId, result)
        return result
    }
}
