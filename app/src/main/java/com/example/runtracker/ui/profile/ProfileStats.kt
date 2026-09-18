package com.example.runtracker.ui.profile

import com.example.runtracker.domain.model.Activity
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.math.roundToInt

private val ZONE: ZoneId = ZoneId.systemDefault()

/** Số liệu một ngày (0 nếu không có buổi tập nào). */
data class DayStat(
    val date: LocalDate,
    val distanceMeters: Double = 0.0,
    val movingTime: Duration = Duration.ZERO,
    val steps: Int = 0,
    val avgHeartRate: Int? = null,
    val calories: Int = 0,
) {
    val hasActivity: Boolean get() = distanceMeters > 0.0
}

/** Tổng hợp một khoảng thời gian (tuần/tháng) + % thay đổi so với khoảng trước đó. */
data class PeriodStat(
    val distanceMeters: Double = 0.0,
    val movingTime: Duration = Duration.ZERO,
    val steps: Int = 0,
    val avgHeartRate: Int? = null,
    val calories: Int = 0,
    /** % so với khoảng liền trước; null nếu khoảng trước không có dữ liệu để so sánh. */
    val deltaPct: Double? = null,
)

/** 7 ngày Thứ Hai → Chủ Nhật của tuần bắt đầu tại [weekStart]. */
fun weekDays(activities: List<Activity>, weekStart: LocalDate): List<DayStat> {
    val byDate = activities.groupBy { it.startTime.atZone(ZONE).toLocalDate() }
    return (0..6).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        dayStatFor(date, byDate[date].orEmpty())
    }
}

/** Số liệu từng ngày có buổi tập trong [month] (chỉ những ngày có dữ liệu, không phải cả tháng). */
fun monthDays(activities: List<Activity>, month: YearMonth): Map<LocalDate, DayStat> {
    val byDate = activities.groupBy { it.startTime.atZone(ZONE).toLocalDate() }
    return byDate.filterKeys { YearMonth.from(it) == month }
        .mapValues { (date, acts) -> dayStatFor(date, acts) }
}

/** Tổng hợp [activities] rơi trong [range], kèm % so với [previousRange]. */
fun periodStat(activities: List<Activity>, range: ClosedRange<LocalDate>, previousRange: ClosedRange<LocalDate>): PeriodStat {
    val inRange = activities.filter { it.startTime.atZone(ZONE).toLocalDate() in range }
    val inPrev = activities.filter { it.startTime.atZone(ZONE).toLocalDate() in previousRange }

    val distance = inRange.sumOf { it.distanceMeters }
    val moving = inRange.fold(Duration.ZERO) { acc, a -> acc + a.movingTime }
    val steps = inRange.sumOf { it.steps ?: 0 }
    val hrValues = inRange.mapNotNull { it.avgHeartRate }
    val avgHr = if (hrValues.isEmpty()) null else hrValues.average().roundToInt()
    val calories = inRange.sumOf { it.calories ?: 0 }

    val prevDistance = inPrev.sumOf { it.distanceMeters }
    val deltaPct = if (prevDistance > 0.0) (distance - prevDistance) / prevDistance * 100.0 else null

    return PeriodStat(distance, moving, steps, avgHr, calories, deltaPct)
}

/** Quãng đường (km) từng tháng dương lịch, cũ → mới, dài [months] phần tử — cho chart xu hướng. */
fun monthlyTrend(activities: List<Activity>, months: Int): List<Double> {
    val today = LocalDate.now(ZONE)
    val firstMonth = YearMonth.from(today).minusMonths((months - 1).toLong())
    val perMonth = DoubleArray(months)
    activities.forEach { a ->
        val month = YearMonth.from(a.startTime.atZone(ZONE).toLocalDate())
        if (!month.isBefore(firstMonth)) {
            val idx = ChronoUnit.MONTHS.between(firstMonth, month).toInt()
            if (idx in 0 until months) perMonth[idx] += a.distanceMeters / 1000.0
        }
    }
    return perMonth.toList()
}

private fun dayStatFor(date: LocalDate, acts: List<Activity>): DayStat {
    if (acts.isEmpty()) return DayStat(date)
    val distance = acts.sumOf { it.distanceMeters }
    val moving = acts.fold(Duration.ZERO) { acc, a -> acc + a.movingTime }
    val steps = acts.sumOf { it.steps ?: 0 }
    val hrValues = acts.mapNotNull { it.avgHeartRate }
    val avgHr = if (hrValues.isEmpty()) null else hrValues.average().roundToInt()
    val calories = acts.sumOf { it.calories ?: 0 }
    return DayStat(date, distance, moving, steps, avgHr, calories)
}
