package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.tracking.GpxWriter
import javax.inject.Inject

/** Dựng nội dung GPX cho một buổi tập; null nếu không tìm thấy activity hoặc không có trace GPS. */
class ExportActivityGpxUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
) {
    suspend operator fun invoke(activityId: String): String? {
        val activity = activityRepository.getActivity(activityId) ?: return null
        val points = activityRepository.getRoutePoints(activityId)
        if (points.isEmpty()) return null
        return GpxWriter.write(activity, points)
    }
}
