package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.training.CalorieEstimator
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Tạo activity nhập thủ công (không qua GPS — ví dụ chạy treadmill, hoặc quên bật app lúc chạy
 * ngoài trời): người dùng gõ thẳng quãng đường + thời gian, tự tính pace/tốc độ/calo giống buổi
 * ghi GPS bình thường. Không có route points nên KHÔNG dò segment/best-effort (cả hai cần trace);
 * vẫn tính kỷ lục toàn-buổi (pace/quãng đường/độ cao — chỉ cần field Activity) và cập nhật training
 * load như buổi tập thật, để không tạo ra "vùng tối" trong Fitness/Thành tích chỉ vì thiếu GPS.
 * Vẫn xét huy hiệu (mốc quãng đường/chuỗi ngày/marathon/chim sớm — chỉ cần field Activity, không
 * cần route points).
 */
class CreateManualActivityUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
    private val refreshTrainingMetrics: RefreshTrainingMetricsUseCase,
    private val detectActivityRecords: DetectActivityRecordsUseCase,
    private val updateChallengeProgress: UpdateChallengeProgressUseCase,
    private val detectNewBadges: DetectNewBadgesUseCase,
) {
    suspend operator fun invoke(
        type: ActivityType,
        startTime: Instant,
        distanceMeters: Double,
        durationSeconds: Long,
        perceivedExertion: Int?,
    ): String? {
        if (distanceMeters <= 0.0 || durationSeconds <= 0) return null

        val id = UUID.randomUUID().toString()
        val endTime = startTime.plusSeconds(durationSeconds)
        val avgPaceSecPerKm = durationSeconds / (distanceMeters / 1000.0)
        val avgSpeedKmh = (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
        val calories = CalorieEstimator.estimate(
            type = type,
            distanceMeters = distanceMeters,
            movingTimeSeconds = durationSeconds,
            weightKg = userRepository.getCurrentUser()?.weightKg,
        )

        activityRepository.upsertActivity(
            Activity(
                id = id,
                userId = LOCAL_USER_ID,
                type = type,
                startTime = startTime,
                endTime = endTime,
                distanceMeters = distanceMeters,
                duration = durationSeconds.seconds,
                movingTime = durationSeconds.seconds,
                avgPaceSecPerKm = avgPaceSecPerKm,
                avgSpeedKmh = avgSpeedKmh,
                elevationGainMeters = 0.0,
                elevationLossMeters = 0.0,
                avgHeartRate = null,
                maxHeartRate = null,
                calories = calories,
                steps = null,
                avgCadence = null,
                perceivedExertion = perceivedExertion,
                weather = null,
                gpxRawPath = null,
            ),
        )

        detectActivityRecords(id)
        detectNewBadges(id)
        updateChallengeProgress()
        refreshTrainingMetrics(startTime.atZone(ZoneId.systemDefault()).toLocalDate())
        return id
    }
}
