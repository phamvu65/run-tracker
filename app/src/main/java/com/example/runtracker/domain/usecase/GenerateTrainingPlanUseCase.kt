package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.data.training.TrainingGoalStore
import com.example.runtracker.domain.model.TrainingPlan
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.training.TrainingPlanGenerator
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Sinh kế hoạch từ mục tiêu đã lưu + fitness hiện tại (CTL mới nhất) + quãng đường TB 4 tuần gần đây.
 * @return null nếu chưa đặt mục tiêu hoặc giải đã qua.
 */
class GenerateTrainingPlanUseCase @Inject constructor(
    private val goalStore: TrainingGoalStore,
    private val trainingLoadRepository: TrainingLoadRepository,
    private val activityRepository: ActivityRepository,
) {
    suspend operator fun invoke(
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): TrainingPlan? {
        val goal = goalStore.goal() ?: return null
        if (goal.raceDate.isBefore(today)) return null

        val ctl = trainingLoadRepository.getLatestSnapshot(LOCAL_USER_ID)?.ctl ?: 0.0

        val from = today.minusDays(28).atStartOfDay(zoneId).toInstant()
        val to = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        val recent = activityRepository.getActivitiesBetween(LOCAL_USER_ID, from, to)
        val recentWeeklyKm = recent.sumOf { it.distanceMeters } / 1_000.0 / 4.0

        return TrainingPlanGenerator.generate(goal, today, ctl, recentWeeklyKm)
    }
}
