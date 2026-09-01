package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.DailyTrainingLoad
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.training.TrimpCalculator
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject

/**
 * Tính lại [DailyTrainingLoad] của một ngày: tổng TRIMP + thống kê của mọi activity trong ngày.
 * Gọi mỗi khi một activity được chốt số liệu (hoặc RPE thay đổi).
 */
class UpdateDailyTrainingLoadUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
    private val trainingLoadRepository: TrainingLoadRepository,
) {
    suspend operator fun invoke(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()) {
        val user = userRepository.getCurrentUser()
        val year = Year.now(zoneId).value
        val from = date.atStartOfDay(zoneId).toInstant()
        val to = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        val activities = activityRepository.getActivitiesBetween(LOCAL_USER_ID, from, to)
        val trimpTotal = activities.sumOf { TrimpCalculator.forActivity(it, user, year) ?: 0.0 }

        trainingLoadRepository.upsertDailyLoad(
            DailyTrainingLoad(
                userId = LOCAL_USER_ID,
                date = date,
                trimpScore = trimpTotal,
                activityCount = activities.size,
                totalDurationSeconds = activities.sumOf { it.duration.inWholeSeconds },
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
