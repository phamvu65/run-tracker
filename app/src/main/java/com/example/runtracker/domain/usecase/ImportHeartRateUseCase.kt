package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.health.HeartRateSource
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.training.summary
import java.time.ZoneId
import javax.inject.Inject

/**
 * Nạp mẫu nhịp tim cho một activity từ [HeartRateSource] (khoảng [startTime, endTime]),
 * lưu mẫu, đặt avg/max cho activity, rồi cập nhật lại training metrics (TRIMP giờ có HR).
 */
class ImportHeartRateUseCase @Inject constructor(
    private val heartRateSource: HeartRateSource,
    private val activityRepository: ActivityRepository,
    private val refreshTrainingMetrics: RefreshTrainingMetricsUseCase,
) {
    /** @return số mẫu nhập được (0 nếu không có / không có quyền). */
    suspend operator fun invoke(activityId: String): Int {
        val activity = activityRepository.getActivity(activityId) ?: return 0

        val samples = heartRateSource.samplesBetween(activity.startTime, activity.endTime)
        val stats = samples.summary() ?: return 0

        activityRepository.appendHeartRateSamples(activityId, samples)
        activityRepository.upsertActivity(
            activity.copy(avgHeartRate = stats.averageBpm, maxHeartRate = stats.maxBpm),
        )
        refreshTrainingMetrics(activity.startTime.atZone(ZoneId.systemDefault()).toLocalDate())
        return samples.size
    }
}
