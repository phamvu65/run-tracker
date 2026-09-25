package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.Activity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Độ chi tiết nhóm hoạt động theo thời gian cho biểu đồ tiến bộ. */
enum class TrendPeriod { WEEK, MONTH, YEAR }

/** Tổng hợp hoạt động trong 1 khoảng (1 tuần/tháng/năm) — 1 cột trên bar chart. */
data class PeriodBucket(
    val label: String,
    val start: LocalDate,
    val distanceMeters: Double,
    /** Pace trung bình CÓ TRỌNG SỐ (tổng thời gian di chuyển / tổng quãng đường) — null nếu không có hoạt động nào. */
    val avgPaceSecPerKm: Double?,
)

/** Kết quả so sánh 2 khoảng thời gian bất kỳ — xem [ProgressStats.compare]. */
data class PeriodComparison(
    val currentDistanceMeters: Double,
    val previousDistanceMeters: Double,
    /** % thay đổi quãng đường, dương = nhiều hơn — null nếu khoảng trước không có dữ liệu để so. */
    val distanceDeltaPct: Double?,
    val currentAvgPaceSecPerKm: Double?,
    val previousAvgPaceSecPerKm: Double?,
    /** Âm = pace nhanh hơn (cải thiện), dương = chậm hơn — null nếu thiếu dữ liệu 1 trong 2 phía. */
    val paceDeltaSecPerKm: Double?,
)

/**
 * Tổng hợp [Activity] theo khoảng thời gian — dùng cho biểu đồ tiến bộ (mục 3) và so sánh với
 * chính mình (mục 4, chỉ là phép tính % dựa trên cùng phép tổng hợp này, không cần bảng mới).
 * Thuần JVM, có test.
 */
object ProgressStats {

    /**
     * [count] cột gần nhất tính đến [endDate] (bao gồm khoảng chứa [endDate]), cũ nhất trước.
     * Tuần bắt đầu Thứ Hai (cùng quy ước với `ui/profile/ProfileStats.weekDays`).
     */
    fun buckets(
        activities: List<Activity>,
        period: TrendPeriod,
        count: Int,
        endDate: LocalDate = LocalDate.now(),
    ): List<PeriodBucket> {
        if (count <= 0) return emptyList()
        val zone = java.time.ZoneId.systemDefault()
        val byDate = activities.groupBy { it.startTime.atZone(zone).toLocalDate() }

        return (count - 1 downTo 0).map { offset ->
            val (start, end, label) = when (period) {
                TrendPeriod.WEEK -> {
                    val weekStart = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset.toLong())
                    Triple(weekStart, weekStart.plusDays(6), "T${weekStart.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}")
                }
                TrendPeriod.MONTH -> {
                    val month = java.time.YearMonth.from(endDate).minusMonths(offset.toLong())
                    Triple(month.atDay(1), month.atEndOfMonth(), "Th${month.monthValue}")
                }
                TrendPeriod.YEAR -> {
                    val year = endDate.year - offset
                    Triple(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), year.toString())
                }
            }
            val inRange = byDate.filterKeys { it in start..end }.values.flatten()
            val distance = inRange.sumOf { it.distanceMeters }
            val movingSeconds = inRange.sumOf { it.movingTime.inWholeSeconds }
            val pace = if (distance > 0) movingSeconds / (distance / 1000.0) else null
            PeriodBucket(label, start, distance, pace)
        }
    }

    /** So sánh 2 khoảng NGÀY bất kỳ (không nhất thiết liền kề — VD "tháng này" vs "cùng tháng 3 tháng trước"). */
    fun compare(
        activities: List<Activity>,
        currentRange: ClosedRange<LocalDate>,
        previousRange: ClosedRange<LocalDate>,
    ): PeriodComparison {
        val zone = java.time.ZoneId.systemDefault()
        fun aggregate(range: ClosedRange<LocalDate>): Pair<Double, Double?> {
            val inRange = activities.filter { it.startTime.atZone(zone).toLocalDate() in range }
            val distance = inRange.sumOf { it.distanceMeters }
            val movingSeconds = inRange.sumOf { it.movingTime.inWholeSeconds }
            val pace = if (distance > 0) movingSeconds / (distance / 1000.0) else null
            return distance to pace
        }

        val (currentDistance, currentPace) = aggregate(currentRange)
        val (previousDistance, previousPace) = aggregate(previousRange)
        val distanceDelta = if (previousDistance > 0) (currentDistance - previousDistance) / previousDistance * 100.0 else null
        val paceDelta = if (currentPace != null && previousPace != null) currentPace - previousPace else null

        return PeriodComparison(
            currentDistanceMeters = currentDistance,
            previousDistanceMeters = previousDistance,
            distanceDeltaPct = distanceDelta,
            currentAvgPaceSecPerKm = currentPace,
            previousAvgPaceSecPerKm = previousPace,
            paceDeltaSecPerKm = paceDelta,
        )
    }
}
