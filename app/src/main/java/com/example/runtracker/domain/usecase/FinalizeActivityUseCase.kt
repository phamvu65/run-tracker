package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.tracking.LapCalculator
import com.example.runtracker.domain.tracking.RunAggregator
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
    private val refreshTrainingMetrics: RefreshTrainingMetricsUseCase,
) {
    /**
     * @param endTime null -> lấy timestamp điểm GPS cuối (đúng cho buổi bị gián đoạn), fallback now.
     * @param pausedSeconds tổng thời gian đã tạm dừng, trừ khỏi tổng thời gian.
     */
    suspend operator fun invoke(
        activityId: String,
        endTime: Instant? = null,
        pausedSeconds: Long = 0,
    ) {
        val activity = repository.getActivity(activityId) ?: return
        val points = repository.getRoutePoints(activityId)
        val aggregate = RunAggregator.fromPoints(points)
        val end = endTime ?: points.lastOrNull()?.timestamp ?: Instant.now()
        val totalSeconds = (Duration.between(activity.startTime, end).seconds - pausedSeconds)
            .coerceAtLeast(0)

        repository.upsertActivity(
            activity.copy(
                endTime = end,
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
            ),
        )
        repository.replaceLaps(activityId, LapCalculator.splitByDistance(points))

        val date = activity.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
        refreshTrainingMetrics(date)
    }
}
