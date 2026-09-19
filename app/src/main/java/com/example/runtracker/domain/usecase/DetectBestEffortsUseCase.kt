package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.BestEffortRepository
import javax.inject.Inject

/**
 * Sau khi một activity được chốt số liệu: tính "best effort" theo từng cự ly chuẩn đạt được
 * (1K/5K/10K/bán marathon), xếp hạng so với lịch sử rồi lưu — dùng cho huy chương thành tích trên
 * feed hoạt động.
 */
class DetectBestEffortsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val bestEffortRepository: BestEffortRepository,
) {
    suspend operator fun invoke(activityId: String) {
        val activity = activityRepository.getActivity(activityId) ?: return
        val points = activityRepository.getRoutePoints(activityId)
        if (points.size < 2) return
        bestEffortRepository.recomputeForActivity(activity.userId, activityId, points, activity.startTime)
    }
}
