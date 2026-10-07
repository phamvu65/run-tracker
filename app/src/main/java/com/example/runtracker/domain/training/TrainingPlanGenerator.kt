package com.example.runtracker.domain.training

import com.example.runtracker.core.formatPace
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
 * Thông tin Pace gợi ý cho các bài tập dựa trên Pace thi đấu mục tiêu.
 */
data class PaceInfo(
    val racePaceSec: Double,
    val easyPaceMinSec: Double,
    val easyPaceMaxSec: Double,
    val tempoPaceSec: Double,
    val intervalPaceSec: Double,
)

/**
 * Sinh kế hoạch tập luyện đa tuần chuẩn thể thao hướng tới giải chạy:
 * 1. Áp dụng **Quy tắc 10% (Progressive Overload)**: Khối lượng km tuần tăng không quá 10%/tuần.
 * 2. Cứ mỗi 3 tuần tăng tải sẽ có 1 **Tuần hồi phục** (giảm 20% km) để cơ thể hấp thụ bài tập.
 * 3. Cho phép linh hoạt **Số buổi tập/tuần (3, 4, hoặc 5 buổi)** và **Ngày Long Run (Thứ 7 hoặc CN)**.
 * 4. Tự động tính **Pace mục tiêu cụ thể** cho từng bài tập (Easy, Tempo, Interval) nếu có Target Time.
 * 5. Giảm tải (Taper) chuẩn theo cự ly (5K/10K taper 1 tuần, 21K/42K taper 2-3 tuần).
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
        val distKm = dist.meters / 1000.0
        val effectiveRecentKm = maxOf(recentWeeklyKm, currentCtl * CTL_TO_WEEKLY_KM)
        val peakKm = peakWeeklyKm(dist, effectiveRecentKm, goal.daysPerWeek)
        val startKm = startWeeklyKm(effectiveRecentKm, peakKm)

        // Taper tuần: 5K/10K -> 1 tuần; 21K/42K -> 2-3 tuần
        val taperWeeks = when {
            !includesRace -> 0
            dist == RaceDistance.FIVE_K || dist == RaceDistance.TEN_K -> if (totalWeeks >= 3) 1 else 0
            totalWeeks >= 6 -> 2
            totalWeeks >= 3 -> 1
            else -> 0
        }

        // Tính Pace mục tiêu nếu người dùng nhập thời gian đích
        val paceInfo = goal.targetTimeSeconds?.let { targetSec ->
            if (targetSec > 0) calculatePaceInfo(distKm, targetSec) else null
        }

        // Tự động tính khối lượng km tuần theo Quy tắc 10% (Progressive Overload)
        val weeklyVolumes = calculateWeeklyVolumes(
            totalWeeks = totalWeeks,
            startKm = startKm,
            peakKm = peakKm,
            taperWeeks = taperWeeks,
            includesRace = includesRace,
            distKm = distKm,
        )

        val weeks = (0 until totalWeeks).map { i ->
            val weekStart = thisWeekMonday.plusWeeks(i.toLong())
            val phase = phaseFor(i, totalWeeks, taperWeeks, includesRace)
            val weeklyKm = weeklyVolumes[i]

            val sessions = sessionsForWeek(
                weekStart = weekStart,
                weekIndex = i,
                totalWeeks = totalWeeks,
                phase = phase,
                weeklyKm = weeklyKm,
                goal = goal,
                paceInfo = paceInfo,
            )

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

    private fun calculateWeeklyVolumes(
        totalWeeks: Int,
        startKm: Double,
        peakKm: Double,
        taperWeeks: Int,
        includesRace: Boolean,
        distKm: Double,
    ): List<Double> {
        val volumes = ArrayList<Double>(totalWeeks)
        var currentVol = startKm
        var buildCount = 0

        for (i in 0 until totalWeeks) {
            val phase = phaseFor(i, totalWeeks, taperWeeks, includesRace)
            val vol = when (phase) {
                TrainingPhase.RACE -> (distKm + startKm * 0.25).coerceAtLeast(distKm)
                TrainingPhase.TAPER -> {
                    val weeksToRace = totalWeeks - 1 - i
                    val factor = if (weeksToRace == 0) 0.45 else 0.70
                    peakKm * factor
                }
                TrainingPhase.PEAK -> peakKm
                else -> {
                    if (buildCount > 0 && buildCount % 4 == 3) {
                        // Tuần hồi phục: giảm 20% km so với tuần trước
                        (currentVol * 0.80).coerceAtLeast(startKm * 0.8)
                    } else {
                        // Tăng tải tối đa 8-10% mỗi tuần (Tịnh tiến an toàn)
                        val next = if (i == 0) startKm else (currentVol * 1.08).coerceAtMost(peakKm)
                        currentVol = next
                        next
                    }
                }
            }
            if (phase == TrainingPhase.BASE || phase == TrainingPhase.BUILD) {
                buildCount++
            }
            volumes.add(vol.coerceAtLeast(distKm * 0.6))
        }
        return volumes
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
        weekIndex: Int,
        totalWeeks: Int,
        phase: TrainingPhase,
        weeklyKm: Double,
        goal: TrainingGoal,
        paceInfo: PaceInfo?,
    ): List<PlannedSession> {
        val raceKm = goal.raceDistance.meters / 1000.0

        if (phase == TrainingPhase.RACE) {
            return (0..6).map { d ->
                val date = weekStart.plusDays(d.toLong())
                when {
                    date == goal.raceDate -> PlannedSession(
                        date, SessionType.RACE, raceKm.round1(),
                        "🏁 ${goal.raceDistance.label} — ngày thi đấu! Target Pace: ${paceInfo?.let { formatPace(it.racePaceSec) } ?: "chỉnh đều tay"}",
                    )
                    date == goal.raceDate.minusDays(1) -> PlannedSession(
                        date, SessionType.REST, null, "Nghỉ ngơi hoàn toàn, nạp carb, ngủ sớm",
                    )
                    date.isBefore(goal.raceDate) -> PlannedSession(
                        date, SessionType.EASY, 3.0, "Chạy nhẹ 3 km giữ chân ${paceText(paceInfo?.easyPaceMinSec, paceInfo?.easyPaceMaxSec)}",
                    )
                    else -> PlannedSession(date, SessionType.REST, null, "Nghỉ ngơi hồi phục sau giải")
                }
            }
        }

        val taperWeeks = when {
            goal.raceDistance == RaceDistance.FIVE_K || goal.raceDistance == RaceDistance.TEN_K -> if (totalWeeks >= 3) 1 else 0
            totalWeeks >= 6 -> 2
            totalWeeks >= 3 -> 1
            else -> 0
        }

        // Tự động tính cự ly bài Long Run tăng dần theo tuần
        val maxLong = maxLongRunKm(goal.raceDistance)
        val longRunKm = when (phase) {
            TrainingPhase.PEAK -> maxLong
            TrainingPhase.TAPER -> (maxLong * 0.6).round1()
            else -> {
                val progress = (weekIndex.toDouble() / (totalWeeks - taperWeeks - PEAK_WEEKS).coerceAtLeast(1)).coerceIn(0.2, 1.0)
                (maxLong * progress).coerceIn(5.0, maxLong).round1()
            }
        }

        val (qualityType, qualityRaw) = when (phase) {
            TrainingPhase.BUILD -> SessionType.TEMPO to minOf(10.0, weeklyKm * 0.22).coerceAtLeast(4.0)
            TrainingPhase.PEAK -> SessionType.INTERVAL to minOf(10.0, weeklyKm * 0.22).coerceAtLeast(4.0)
            TrainingPhase.TAPER -> SessionType.RACE_PACE to minOf(8.0, weeklyKm * 0.2).coerceAtLeast(3.0)
            else -> SessionType.EASY to (weeklyKm * 0.15).coerceAtLeast(4.0)
        }
        val qualityKm = qualityRaw.round1()
        val remaining = (weeklyKm - longRunKm - qualityKm).coerceAtLeast(0.0)

        // Phân bổ các ngày chạy và ngày nghỉ theo số buổi/tuần (3, 4 hoặc 5 buổi) và ngày Long Run
        val daysCount = goal.daysPerWeek.coerceIn(3, 5)
        val easyRunCount = (daysCount - 2).coerceAtLeast(1)
        val easyKm = (remaining / easyRunCount).coerceAtLeast(3.0).round1()

        val longRunOffset = if (goal.longRunDay == DayOfWeek.SATURDAY) 5 else 6

        return (0..6).map { d ->
            val date = weekStart.plusDays(d.toLong())
            val sessionType = when {
                d == longRunOffset -> SessionType.LONG_RUN
                d == 2 -> qualityType // Thứ 4: Bài tập chất lượng
                d == 1 && daysCount >= 4 -> SessionType.EASY // Thứ 3
                d == 3 && daysCount >= 3 -> SessionType.EASY // Thứ 5
                d == 5 && longRunOffset == 6 && daysCount >= 5 -> SessionType.EASY // Thứ 7
                d == 6 && longRunOffset == 5 && daysCount >= 5 -> SessionType.EASY // Chủ Nhật
                else -> SessionType.REST
            }

            val distance = when (sessionType) {
                SessionType.LONG_RUN -> longRunKm
                SessionType.EASY -> easyKm
                SessionType.REST -> null
                SessionType.RACE -> raceKm
                else -> qualityKm
            }

            val desc = when (sessionType) {
                SessionType.LONG_RUN -> "Long run $longRunKm km ở pace nhẹ nhàng ${paceText(paceInfo?.easyPaceMinSec, paceInfo?.easyPaceMaxSec)}, nạp điện giải/gel"
                SessionType.EASY -> "Chạy nhẹ $easyKm km ${paceText(paceInfo?.easyPaceMinSec, paceInfo?.easyPaceMaxSec)}, thả lỏng"
                SessionType.REST -> "Nghỉ ngơi hoặc đi bộ, giãn cơ, tập core nhẹ"
                else -> qualityDescription(qualityType, qualityKm, paceInfo)
            }

            PlannedSession(date, sessionType, distance, desc)
        }
    }

    private fun calculatePaceInfo(distKm: Double, targetSec: Long): PaceInfo {
        val racePaceSec = (targetSec.toDouble() / distKm).coerceAtLeast(180.0) // ít nhất 3:00/km
        return PaceInfo(
            racePaceSec = racePaceSec,
            easyPaceMinSec = racePaceSec + 45.0,
            easyPaceMaxSec = racePaceSec + 75.0,
            tempoPaceSec = (racePaceSec - 10.0).coerceAtLeast(180.0),
            intervalPaceSec = (racePaceSec - 25.0).coerceAtLeast(160.0),
        )
    }

    private fun paceText(minSec: Double?, maxSec: Double?): String {
        if (minSec == null || maxSec == null) return ""
        return "(Pace ${formatPace(minSec)}–${formatPace(maxSec)})"
    }

    private fun qualityDescription(type: SessionType, km: Double, paceInfo: PaceInfo?): String = when (type) {
        SessionType.TEMPO -> {
            val paceStr = paceInfo?.let { "ở Pace ${formatPace(it.tempoPaceSec)}" } ?: "ở pace ngưỡng"
            "Tempo: 1.5km khởi động + ${km - 2}km $paceStr + 0.5km thả lỏng"
        }
        SessionType.INTERVAL -> {
            val paceStr = paceInfo?.let { "ở Pace ${formatPace(it.intervalPaceSec)}" } ?: "nhanh"
            "Interval: khởi động + 6–8 × 800m $paceStr (nghỉ đi bộ 200m) + thả lỏng (~$km km tổng)"
        }
        SessionType.RACE_PACE -> {
            val paceStr = paceInfo?.let { "ở Pace thi đấu ${formatPace(it.racePaceSec)}" } ?: "ở pace mục tiêu"
            "$km km trong đó 2–3 km $paceStr để cảm nhận nhịp chân"
        }
        else -> "Chạy nhẹ $km km + 4–6 đoạn bứt tốc 20 giây"
    }

    private fun peakWeeklyKm(dist: RaceDistance, effectiveRecentKm: Double, daysPerWeek: Int): Double {
        val (min, max) = when (dist) {
            RaceDistance.FIVE_K -> 18.0 to 35.0
            RaceDistance.TEN_K -> 25.0 to 48.0
            RaceDistance.HALF -> 35.0 to 65.0
            RaceDistance.FULL -> 48.0 to 90.0
        }
        val daysMultiplier = daysPerWeek / 4.0
        return (effectiveRecentKm * 1.25 * daysMultiplier).coerceIn(min, max)
    }

    private fun startWeeklyKm(effectiveRecentKm: Double, peakKm: Double): Double =
        maxOf(effectiveRecentKm, peakKm * 0.50).coerceAtMost(peakKm)

    private fun maxLongRunKm(dist: RaceDistance): Double = when (dist) {
        RaceDistance.FIVE_K -> 10.0
        RaceDistance.TEN_K -> 15.0
        RaceDistance.HALF -> 20.0
        RaceDistance.FULL -> 32.0
    }

    private fun Double.round1(): Double = (this * 2).roundToInt() / 2.0
}
