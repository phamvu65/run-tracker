package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.tracking.LapCalculator
import com.example.runtracker.domain.tracking.RunAggregator
import com.example.runtracker.domain.training.CalorieEstimator
import com.example.runtracker.domain.training.summary
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Chốt số liệu cuối cho một buổi tập: tính lại aggregate + lap từ trace đã lưu, ghi đè Activity,
 * rồi cập nhật training load của ngày. Dùng cho cả STOP bình thường và "kết thúc" buổi bị gián đoạn.
 */
class FinalizeActivityUseCase @Inject constructor(
    private val repository: ActivityRepository,
    private val userRepository: UserRepository,
    private val refreshTrainingMetrics: RefreshTrainingMetricsUseCase,
    private val detectSegmentEfforts: DetectSegmentEffortsUseCase,
    private val updateChallengeProgress: UpdateChallengeProgressUseCase,
    private val detectBestEfforts: DetectBestEffortsUseCase,
) {
    /**
     * @param endTime null -> lấy timestamp điểm GPS cuối (đúng cho buổi bị gián đoạn), fallback now.
     * @param pausedSeconds tổng thời gian đã tạm dừng, trừ khỏi tổng thời gian.
     * @param steps số bước đếm được từ cảm biến trong buổi tập (null nếu không có, ví dụ buổi bị
     *   gián đoạn kết thúc thủ công — không có nguồn persisted để tính lại như route points).
     */
    suspend operator fun invoke(
        activityId: String,
        endTime: Instant? = null,
        pausedSeconds: Long = 0,
        steps: Int? = null,
    ) {
        val activity = repository.getActivity(activityId) ?: return
        val points = repository.getRoutePoints(activityId)
        val aggregate = RunAggregator.fromPoints(points)
        val hr = repository.getHeartRateSamples(activityId).summary()
        val end = endTime ?: points.lastOrNull()?.timestamp ?: Instant.now()
        val totalSeconds = (Duration.between(activity.startTime, end).seconds - pausedSeconds)
            .coerceAtLeast(0)
        val calories = CalorieEstimator.estimate(
            type = activity.type,
            distanceMeters = aggregate.distanceMeters,
            movingTimeSeconds = aggregate.movingTimeSeconds,
            weightKg = userRepository.getCurrentUser()?.weightKg,
        )

        repository.upsertActivity(
            activity.copy(
                endTime = end,
                avgHeartRate = hr?.averageBpm ?: activity.avgHeartRate,
                maxHeartRate = hr?.maxBpm ?: activity.maxHeartRate,
                distanceMeters = aggregate.distanceMeters,
                duration = totalSeconds.seconds,
                movingTime = aggregate.movingTimeSeconds.seconds,
                avgPaceSecPerKm = if (aggregate.distanceMeters > 0) {
                    aggregate.movingTimeSeconds / (aggregate.distanceMeters / 1000.0)
                } else {
                    0.0
                },
                avgSpeedKmh = if (totalSeconds > 0) {
                    (aggregate.distanceMeters / 1000.0) / (totalSeconds / 3600.0)
                } else {
                    0.0
                },
                elevationGainMeters = aggregate.elevationGainMeters,
                elevationLossMeters = aggregate.elevationLossMeters,
                calories = calories ?: activity.calories,
                steps = steps ?: activity.steps,
            ),
        )
        repository.replaceLaps(activityId, LapCalculator.splitByDistance(points))

        detectSegmentEfforts(activityId)
        detectBestEfforts(activityId)
        updateChallengeProgress()

        val date = activity.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
        refreshTrainingMetrics(date)
    }
}
