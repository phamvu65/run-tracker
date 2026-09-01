package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.training.FitnessFreshnessCalculator
import java.time.LocalDate
import javax.inject.Inject

/**
 * Cuộn EWMA CTL/ATL/TSB tới ngày [upTo]. Bắt đầu lại từ ngày của snapshot mới nhất
 * (để activity/RPE về muộn trong ngày đó vẫn được tính), điền ngày nghỉ với TRIMP = 0
 * để fitness/fatigue tự suy giảm.
 */
class RecalculateFitnessFreshnessUseCase @Inject constructor(
    private val trainingLoadRepository: TrainingLoadRepository,
) {
    suspend operator fun invoke(upTo: LocalDate = LocalDate.now()) {
        val earliest = trainingLoadRepository.getEarliestDailyLoadDate(LOCAL_USER_ID) ?: return
        val latest = trainingLoadRepository.getLatestSnapshot(LOCAL_USER_ID)

        var day = when {
            latest == null -> earliest
            latest.date.isBefore(upTo) -> latest.date
            else -> upTo // latest.date >= upTo: chỉ tính lại đúng ngày đó
        }
        if (day.isAfter(upTo)) return

        var previous = trainingLoadRepository.getSnapshot(LOCAL_USER_ID, day.minusDays(1))
        while (!day.isAfter(upTo)) {
            val trimp = trainingLoadRepository.getDailyLoad(LOCAL_USER_ID, day)?.trimpScore ?: 0.0
            val snapshot = FitnessFreshnessCalculator.next(previous, LOCAL_USER_ID, day, trimp)
            trainingLoadRepository.upsertSnapshot(snapshot)
            previous = snapshot
            day = day.plusDays(1)
        }
    }
}
