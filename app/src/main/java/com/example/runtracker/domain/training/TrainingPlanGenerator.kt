package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.PlanWeek
import com.example.runtracker.domain.model.PlannedSession
import com.example.runtracker.domain.model.SessionType
import com.example.runtracker.domain.model.TrainingGoal
import com.example.runtracker.domain.model.TrainingPhase
import com.example.runtracker.domain.model.TrainingPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/**
 * Sinh kế hoạch tập nhiều tuần hướng tới một giải chạy, theo chu kỳ Base → Build → Peak → Taper.
 * Khối lượng tuần tăng dần từ mức gần đây của người dùng (quãng đường TB 4 tuần, có tính tới CTL)
 * lên mức đỉnh phù hợp cự ly, chèn tuần hồi phục mỗi 4 tuần, rồi giảm tải (taper) trước giải.
 *
 * Nếu giải xa hơn [MAX_WEEKS] tuần: chỉ lên [MAX_WEEKS] tuần "xây nền" từ bây giờ (chưa taper),
 * chu kỳ đầy đủ sẽ xuất hiện khi giải vào trong tầm [MAX_WEEKS] tuần.
 *
 * Thuần JVM — không phụ thuộc Android.
 */
object TrainingPlanGenerator {

    const val MAX_WEEKS = 24
    private const val PEAK_WEEKS = 2
    private const val CTL_TO_WEEKLY_KM = 0.6

    fun generate(
        goal: TrainingGoal,
        today: LocalDate,
        currentCtl: Double,
        recentWeeklyKm: Double,
    ): TrainingPlan {
        val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val raceWeekMonday = goal.raceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val weeksUntilRace = (ChronoUnit.WEEKS.between(thisWeekMonday, raceWeekMonday).toInt() + 1)
            .coerceAtLeast(1)
        val includesRace = weeksUntilRace <= MAX_WEEKS
        val totalWeeks = weeksUntilRace.coerceAtMost(MAX_WEEKS)

        val dist = goal.raceDistance
        val effectiveRecentKm = maxOf(recentWeeklyKm, currentCtl * CTL_TO_WEEKLY_KM)
        val peakKm = peakWeeklyKm(dist, effectiveRecentKm)
        val startKm = startWeeklyKm(effectiveRecentKm, peakKm)

        val taperWeeks = when {
            !includesRace -> 0
            totalWeeks >= 4 -> 2
            totalWeeks >= 2 -> 1
            else -> 0
        }
        val rampWeeks = (totalWeeks - 1 - taperWeeks - PEAK_WEEKS).coerceAtLeast(1)

        val weeks = (0 until totalWeeks).map { i ->
            val weekStart = thisWeekMonday.plusWeeks(i.toLong())
            val weeksToRace = totalWeeks - 1 - i
            val phase = phaseFor(i, totalWeeks, taperWeeks, includesRace)

            val rawVolume = when (phase) {
                TrainingPhase.RACE -> dist.meters / 1000.0 + startKm * 0.3
                TrainingPhase.TAPER -> peakKm * (0.4 + 0.2 * weeksToRace).coerceIn(0.4, 0.85)
                TrainingPhase.PEAK -> peakKm
                else -> {
                    val p = (i.toDouble() / rampWeeks).coerceIn(0.0, 1.0)
                    val ramped = startKm + (peakKm - startKm) * p
                    if ((i + 1) % 4 == 0) ramped * 0.8 else ramped
                }
            }
            val weeklyKm = rawVolume.coerceAtLeast(dist.meters / 1000.0 * 0.6)
            val sessions = sessionsForWeek(weekStart, phase, weeklyKm, goal)

            PlanWeek(
                index = i + 1,
                startDate = weekStart,
                phase = phase,
                targetDistanceKm = sessions.sumOf { it.distanceKm ?: 0.0 }.round1(),
                sessions = sessions,
            )
        }

        return TrainingPlan(goal = goal, weeks = weeks)
    }

    private fun phaseFor(
        i: Int,
        totalWeeks: Int,
        taperWeeks: Int,
        includesRace: Boolean,
    ): TrainingPhase {
        val lastIndex = totalWeeks - 1
        if (includesRace) {
            if (i == lastIndex) return TrainingPhase.RACE
            if (i > lastIndex - 1 - taperWeeks) return TrainingPhase.TAPER
            if (i > lastIndex - 1 - taperWeeks - PEAK_WEEKS) return TrainingPhase.PEAK
        }
        val nonTaper = totalWeeks - taperWeeks
        return if (i < nonTaper * 0.5) TrainingPhase.BASE else TrainingPhase.BUILD
    }

    private fun sessionsForWeek(
        weekStart: LocalDate,
        phase: TrainingPhase,
        weeklyKm: Double,
        goal: TrainingGoal,
    ): List<PlannedSession> {
        val raceKm = goal.raceDistance.meters / 1000.0

        if (phase == TrainingPhase.RACE) {
            return (0..6).map { d ->
                val date = weekStart.plusDays(d.toLong())
                when {
                    date == goal.raceDate -> PlannedSession(
                        date, SessionType.RACE, raceKm.round1(),
                        "🏁 ${goal.raceDistance.label} — ngày thi đấu",
                    )
                    date == goal.raceDate.minusDays(1) -> PlannedSession(
                        date, SessionType.REST, null, "Nghỉ, chuẩn bị đồ, ngủ sớm",
                    )
                    date.isBefore(goal.raceDate) -> PlannedSession(
                        date, SessionType.EASY, 3.0, "Chạy nhẹ 3 km giữ chân, vài đoạn tăng tốc ngắn",
                    )
                    else -> PlannedSession(date, SessionType.REST, null, "Nghỉ hồi phục sau giải")
                }
            }
        }

        val longRunKm = minOf(weeklyKm * 0.35, maxLongRunKm(goal.raceDistance))
            .coerceAtLeast(5.0).round1()
        val (qualityType, qualityRaw) = when (phase) {
            TrainingPhase.BUILD -> SessionType.TEMPO to minOf(10.0, weeklyKm * 0.22).coerceAtLeast(5.0)
            TrainingPhase.PEAK -> SessionType.INTERVAL to minOf(10.0, weeklyKm * 0.22).coerceAtLeast(5.0)
            TrainingPhase.TAPER -> SessionType.RACE_PACE to minOf(8.0, weeklyKm * 0.2).coerceAtLeast(4.0)
            else -> SessionType.EASY to (weeklyKm * 0.15).coerceAtLeast(4.0)
        }
        val qualityKm = qualityRaw.round1()
        val remaining = (weeklyKm - longRunKm - qualityKm).coerceAtLeast(0.0)
        val easyKm = (remaining / 3.0).coerceAtLeast(3.0).round1()

        fun day(offset: Int) = weekStart.plusDays(offset.toLong())
        return listOf(
            PlannedSession(day(0), SessionType.REST, null, "Nghỉ hoặc đi bộ, giãn cơ"),
            PlannedSession(day(1), SessionType.EASY, easyKm, "Chạy nhẹ $easyKm km, ở mức có thể nói chuyện thoải mái"),
            PlannedSession(day(2), qualityType, qualityKm, qualityDescription(qualityType, qualityKm)),
            PlannedSession(day(3), SessionType.EASY, easyKm, "Chạy nhẹ $easyKm km, thả lỏng"),
            PlannedSession(day(4), SessionType.REST, null, "Nghỉ hoặc bổ trợ (core, sức mạnh nhẹ)"),
            PlannedSession(day(5), SessionType.LONG_RUN, longRunKm, "Long run $longRunKm km ở pace dễ, tiếp nước"),
            PlannedSession(day(6), SessionType.EASY, easyKm, "Chạy hồi phục $easyKm km rất nhẹ"),
        )
    }

    private fun qualityDescription(type: SessionType, km: Double): String = when (type) {
        SessionType.TEMPO -> "Tempo: khởi động + $km km liên tục ở pace ngưỡng + thả lỏng"
        SessionType.INTERVAL -> "Interval: khởi động + 6–8 × 800 m nhanh (nghỉ chạy nhẹ), ~$km km tổng"
        SessionType.RACE_PACE -> "$km km trong đó 2–3 km ở pace mục tiêu để giữ cảm giác"
        else -> "Chạy nhẹ $km km + 4–6 đoạn tăng tốc 20 giây cuối buổi"
    }

    private fun peakWeeklyKm(dist: RaceDistance, effectiveRecentKm: Double): Double {
        val (min, max) = when (dist) {
            RaceDistance.FIVE_K -> 20.0 to 40.0
            RaceDistance.TEN_K -> 30.0 to 55.0
            RaceDistance.HALF -> 40.0 to 80.0
            RaceDistance.FULL -> 55.0 to 110.0
        }
        return (effectiveRecentKm * 1.3).coerceIn(min, max)
    }

    private fun startWeeklyKm(effectiveRecentKm: Double, peakKm: Double): Double =
        maxOf(effectiveRecentKm, peakKm * 0.55).coerceAtMost(peakKm)

    private fun maxLongRunKm(dist: RaceDistance): Double = when (dist) {
        RaceDistance.FIVE_K -> 10.0
        RaceDistance.TEN_K -> 16.0
        RaceDistance.HALF -> 22.0
        RaceDistance.FULL -> 32.0
    }

    private fun Double.round1(): Double = (this * 2).roundToInt() / 2.0
}
