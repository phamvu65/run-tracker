package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.PerformancePrediction
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.PerformancePredictionRepository
import com.example.runtracker.domain.training.RaceDistance
import com.example.runtracker.domain.training.RiegelPredictor
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Chọn buổi chạy nhanh nhất (theo pace) trong 90 ngày gần nhất làm nỗ lực gốc,
 * rồi ngoại suy Riegel ra 5K/10K/HALF/FULL. Không có buổi phù hợp → xoá dự đoán.
 */
class UpdatePerformancePredictionsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val predictionRepository: PerformancePredictionRepository,
) {
    suspend operator fun invoke(now: Instant = Instant.now()) {
        val activities = activityRepository.getActivitiesBetween(
            userId = LOCAL_USER_ID,
            from = now.minus(WINDOW_DAYS, ChronoUnit.DAYS),
            to = now,
        )

        val base = activities
            .filter { it.type == ActivityType.RUNNING && it.distanceMeters >= MIN_BASE_METERS }
            .filter { baseTimeSeconds(it) > 0 }
            .minByOrNull { it.avgPaceSecPerKm.takeIf { p -> p > 0.0 } ?: Double.MAX_VALUE }

        if (base == null) {
            predictionRepository.replaceAll(emptyList())
            return
        }

        val baseDistance = base.distanceMeters
        val baseTime = baseTimeSeconds(base)
        val computedAt = now.toEpochMilli()

        predictionRepository.replaceAll(
            RaceDistance.entries.map { race ->
                PerformancePrediction(
                    userId = LOCAL_USER_ID,
                    distanceLabel = race.label,
                    predictedSeconds = RiegelPredictor.predictSeconds(
                        baseDistanceMeters = baseDistance,
                        baseTimeSeconds = baseTime,
                        targetDistanceMeters = race.meters,
                    ),
                    basedOnActivityId = base.id,
                    computedAt = computedAt,
                )
            },
        )
    }

    private fun baseTimeSeconds(activity: Activity): Double {
        val seconds = activity.movingTime.inWholeSeconds.takeIf { it > 0 }
            ?: activity.duration.inWholeSeconds
        return seconds.toDouble()
    }

    private companion object {
        const val WINDOW_DAYS = 90L
        const val MIN_BASE_METERS = 3_000.0
    }
}
