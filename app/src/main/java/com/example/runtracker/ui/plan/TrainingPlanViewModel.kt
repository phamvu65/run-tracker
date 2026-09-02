package com.example.runtracker.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.data.training.TrainingGoalStore
import com.example.runtracker.domain.model.TrainingGoal
import com.example.runtracker.domain.model.TrainingPlan
import com.example.runtracker.domain.training.RaceDistance
import com.example.runtracker.domain.usecase.GenerateTrainingPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrainingPlanViewModel @Inject constructor(
    private val goalStore: TrainingGoalStore,
    private val generateTrainingPlan: GenerateTrainingPlanUseCase,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val state: StateFlow<PlanUiState> = goalStore.goalFlow
        .mapLatest { goal ->
            when {
                goal == null -> PlanUiState.NoGoal
                else -> generateTrainingPlan(today)?.let { PlanUiState.Ready(it) } ?: PlanUiState.NoGoal
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState.Loading)

    fun saveGoal(distance: RaceDistance, raceDate: LocalDate) {
        goalStore.save(TrainingGoal(distance, raceDate))
    }

    fun clearGoal() {
        goalStore.clear()
    }
}

sealed interface PlanUiState {
    data object Loading : PlanUiState
    data object NoGoal : PlanUiState
    data class Ready(val plan: TrainingPlan) : PlanUiState
}
