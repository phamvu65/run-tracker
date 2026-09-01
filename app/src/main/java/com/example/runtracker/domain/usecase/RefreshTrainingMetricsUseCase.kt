package com.example.runtracker.domain.usecase

import java.time.LocalDate
import javax.inject.Inject

/**
 * Cập nhật toàn bộ chỉ số dẫn xuất từ activity sau khi một buổi tập thay đổi:
 * tổng TRIMP của ngày -> cuộn CTL/ATL/TSB tới hôm nay -> dự đoán thành tích.
 */
class RefreshTrainingMetricsUseCase @Inject constructor(
    private val updateDailyTrainingLoad: UpdateDailyTrainingLoadUseCase,
    private val recalculateFitnessFreshness: RecalculateFitnessFreshnessUseCase,
    private val updatePerformancePredictions: UpdatePerformancePredictionsUseCase,
) {
    suspend operator fun invoke(activityDate: LocalDate) {
        updateDailyTrainingLoad(activityDate)
        recalculateFitnessFreshness(LocalDate.now())
        updatePerformancePredictions()
    }
}
