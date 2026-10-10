package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.training.FitnessFreshnessCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Cuộn EWMA CTL/ATL/TSB tới ngày [upTo]. Tự động tính DailyTrainingLoad cho tất cả các ngày
 * có activity (từ activity đầu tiên), điền ngày nghỉ với TRIMP = 0 để fitness/fatigue tự suy giảm.
 */
class RecalculateFitnessFreshnessUseCase @Inject constructor(
    private val trainingLoadRepository: TrainingLoadRepository,
    private val activityRepository: ActivityRepository,
    private val updateDailyTrainingLoad: UpdateDailyTrainingLoadUseCase,
) {
    suspend operator fun invoke(
        upTo: LocalDate = LocalDate.now(),
        fromDate: LocalDate? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val allActivities = activityRepository.observeActivities(LOCAL_USER_ID).first()
        if (allActivities.isEmpty()) return

        val earliestActivityDate = allActivities.minOf {
            it.startTime.atZone(zoneId).toLocalDate()
        }

        val earliestDailyLoad = trainingLoadRepository.getEarliestDailyLoadDate(LOCAL_USER_ID)
        val latestSnapshot = trainingLoadRepository.getLatestSnapshot(LOCAL_USER_ID)

        var day = when {
            fromDate != null -> if (fromDate.isBefore(earliestActivityDate)) earliestActivityDate else fromDate
            latestSnapshot == null -> earliestActivityDate
            earliestDailyLoad == null || earliestDailyLoad.isAfter(earliestActivityDate) -> earliestActivityDate
            latestSnapshot.date.isBefore(upTo) -> latestSnapshot.date
            else -> upTo
        }

        if (day.isAfter(upTo)) return

        var previous = trainingLoadRepository.getSnapshot(LOCAL_USER_ID, day.minusDays(1))
        while (!day.isAfter(upTo)) {
            updateDailyTrainingLoad(day, zoneId)
            val trimp = trainingLoadRepository.getDailyLoad(LOCAL_USER_ID, day)?.trimpScore ?: 0.0
            val snapshot = FitnessFreshnessCalculator.next(previous, LOCAL_USER_ID, day, trimp)
            trainingLoadRepository.upsertSnapshot(snapshot)
            previous = snapshot
            day = day.plusDays(1)
        }
    }
}
