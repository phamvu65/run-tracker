package com.example.runtracker.domain.model

import com.example.runtracker.domain.training.RaceDistance
import java.time.DayOfWeek
import java.time.LocalDate

/** Mục tiêu tập luyện của người dùng — chỉ lưu cái này (SharedPreferences), kế hoạch sinh lại mỗi lần. */
data class TrainingGoal(
    val raceDistance: RaceDistance,
    val raceDate: LocalDate,
    val targetTimeSeconds: Long? = null,
    val daysPerWeek: Int = 4,
    val longRunDay: DayOfWeek = DayOfWeek.SUNDAY,
)

enum class TrainingPhase { BASE, BUILD, PEAK, TAPER, RACE }

enum class SessionType(val label: String) {
    REST("Nghỉ"),
    EASY("Chạy nhẹ"),
    LONG_RUN("Long run"),
    TEMPO("Tempo"),
    INTERVAL("Interval"),
    RACE_PACE("Pace thi đấu"),
    RACE("Thi đấu"),
}

data class PlannedSession(
    val date: LocalDate,
    val type: SessionType,
    /** null với buổi nghỉ. */
    val distanceKm: Double?,
    val description: String,
)

data class PlanWeek(
    /** 1 = tuần hiện tại. */
    val index: Int,
    val startDate: LocalDate,
    val phase: TrainingPhase,
    val targetDistanceKm: Double,
    val sessions: List<PlannedSession>,
)

data class TrainingPlan(
    val goal: TrainingGoal,
    val weeks: List<PlanWeek>,
) {
    val totalWeeks: Int get() = weeks.size

    fun sessionOn(date: LocalDate): PlannedSession? =
        weeks.firstOrNull { date >= it.startDate && date < it.startDate.plusDays(7) }
            ?.sessions?.firstOrNull { it.date == date }
}

/** Gợi ý buổi tập "hôm nay nên làm gì" — dựa trên form (TSB) khi chưa có kế hoạch. */
data class DailySuggestion(
    val type: SessionType,
    val headline: String,
    val rationale: String,
)
