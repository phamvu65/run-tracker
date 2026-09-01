package com.example.runtracker.domain.usecase

import java.time.LocalDate
import javax.inject.Inject

/**
 * Cập nhật toàn bộ chỉ số training sau khi một activity thay đổi:
 * tổng TRIMP của ngày đó -> cuộn lại CTL/ATL/TSB tới hôm nay.
 */
class RefreshTrainingMetricsUseCase @Inject constructor(
    private val updateDailyTrainingLoad: UpdateDailyTrainingLoadUseCase,
    private val recalculateFitnessFreshness: RecalculateFitnessFreshnessUseCase,
) {
    suspend operator fun invoke(activityDate: LocalDate) {
        updateDailyTrainingLoad(activityDate)
        recalculateFitnessFreshness(LocalDate.now())
    }
}
