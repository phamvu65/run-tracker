package com.example.runtracker.data.training

import android.content.Context
import androidx.core.content.edit
import com.example.runtracker.domain.model.TrainingGoal
import com.example.runtracker.domain.training.RaceDistance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu bền mục tiêu tập luyện (cự ly + ngày thi đấu) — kế hoạch chi tiết được sinh lại mỗi lần
 * từ fitness hiện tại nên không cần bảng DB.
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
        return TrainingGoal(distance, LocalDate.ofEpochDay(epochDay))
    }

    private companion object {
        const val KEY_DISTANCE = "race_distance"
        const val KEY_DATE = "race_date_epoch_day"
    }
}
