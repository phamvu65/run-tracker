package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import java.time.LocalDate

/**
 * CTL / ATL / TSB (mô hình Banister). EWMA đệ quy — mỗi ngày tính từ snapshot hôm qua,
 * KHÔNG quét lại toàn bộ lịch sử.
 *
 *   CTL_today = CTL_yest + (TRIMP_today − CTL_yest) / 42
 *   ATL_today = ATL_yest + (TRIMP_today − ATL_yest) / 7
 *   TSB_today = CTL_yest − ATL_yest
 *
 * Thuần JVM.
 */
object FitnessFreshnessCalculator {

    const val CTL_TIME_CONSTANT_DAYS = 42.0
    const val ATL_TIME_CONSTANT_DAYS = 7.0

    /**
     * @param previous snapshot của ĐÚNG ngày hôm qua ([date] − 1); null nếu là ngày đầu tiên.
     *   Caller phải đảm bảo không có khoảng trống — ngày nghỉ vẫn phải gọi với `trimpToday = 0`.
     */
    fun next(
        previous: FitnessFreshnessSnapshot?,
        userId: String,
        date: LocalDate,
        trimpToday: Double,
        computedAt: Long = System.currentTimeMillis(),
    ): FitnessFreshnessSnapshot {
        val ctlYesterday = previous?.ctl ?: 0.0
        val atlYesterday = previous?.atl ?: 0.0

        return FitnessFreshnessSnapshot(
            userId = userId,
            date = date,
            ctl = ctlYesterday + (trimpToday - ctlYesterday) / CTL_TIME_CONSTANT_DAYS,
            atl = atlYesterday + (trimpToday - atlYesterday) / ATL_TIME_CONSTANT_DAYS,
            tsb = ctlYesterday - atlYesterday,
            computedAt = computedAt,
        )
    }
}
