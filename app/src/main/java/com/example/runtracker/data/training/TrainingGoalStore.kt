package com.example.runtracker.data.training

import android.content.Context
import androidx.core.content.edit
import com.example.runtracker.domain.model.TrainingGoal
import com.example.runtracker.domain.training.RaceDistance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu bền mục tiêu tập luyện (cự ly + ngày thi đấu + số buổi/tuần + pace mục tiêu) — kế hoạch chi
 * tiết được sinh lại mỗi lần từ fitness hiện tại nên không cần bảng DB.
 */
@Singleton
class TrainingGoalStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("training_goal", Context.MODE_PRIVATE)

    private val _goal = MutableStateFlow(read())
    val goalFlow: StateFlow<TrainingGoal?> = _goal.asStateFlow()

    fun goal(): TrainingGoal? = _goal.value

    fun save(goal: TrainingGoal) {
        prefs.edit {
            putString(KEY_DISTANCE, goal.raceDistance.label)
            putLong(KEY_DATE, goal.raceDate.toEpochDay())
            if (goal.targetTimeSeconds != null) putLong(KEY_TARGET_TIME, goal.targetTimeSeconds) else remove(KEY_TARGET_TIME)
            putInt(KEY_DAYS_PER_WEEK, goal.daysPerWeek)
            putString(KEY_LONG_RUN_DAY, goal.longRunDay.name)
        }
        _goal.value = goal
    }

    fun clear() {
        prefs.edit { clear() }
        _goal.value = null
    }

    private fun read(): TrainingGoal? {
        val distance = prefs.getString(KEY_DISTANCE, null)?.let { RaceDistance.fromLabel(it) } ?: return null
        val epochDay = prefs.getLong(KEY_DATE, -1L).takeIf { it >= 0 } ?: return null
        val targetTimeSeconds = if (prefs.contains(KEY_TARGET_TIME)) prefs.getLong(KEY_TARGET_TIME, -1L).takeIf { it > 0 } else null
        val daysPerWeek = prefs.getInt(KEY_DAYS_PER_WEEK, 4).coerceIn(3, 5)
        val longRunDayStr = prefs.getString(KEY_LONG_RUN_DAY, DayOfWeek.SUNDAY.name)
        val longRunDay = runCatching { DayOfWeek.valueOf(longRunDayStr!!) }.getOrDefault(DayOfWeek.SUNDAY)

        return TrainingGoal(
            raceDistance = distance,
            raceDate = LocalDate.ofEpochDay(epochDay),
            targetTimeSeconds = targetTimeSeconds,
            daysPerWeek = daysPerWeek,
            longRunDay = longRunDay,
        )
    }

    private companion object {
        const val KEY_DISTANCE = "race_distance"
        const val KEY_DATE = "race_date_epoch_day"
        const val KEY_TARGET_TIME = "target_time_seconds"
        const val KEY_DAYS_PER_WEEK = "days_per_week"
        const val KEY_LONG_RUN_DAY = "long_run_day"
    }
}
