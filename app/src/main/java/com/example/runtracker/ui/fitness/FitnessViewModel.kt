package com.example.runtracker.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.data.training.TrainingGoalStore
import com.example.runtracker.domain.model.DailySuggestion
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import com.example.runtracker.domain.model.PerformancePrediction
import com.example.runtracker.domain.repository.PerformancePredictionRepository
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.training.DailyTrainingSuggestion
import com.example.runtracker.domain.usecase.GenerateTrainingPlanUseCase
import com.example.runtracker.domain.usecase.RecalculateFitnessFreshnessUseCase
import com.example.runtracker.domain.usecase.UpdatePerformancePredictionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val WINDOW_DAYS = 90L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FitnessViewModel @Inject constructor(
    trainingLoadRepository: TrainingLoadRepository,
    predictionRepository: PerformancePredictionRepository,
    goalStore: TrainingGoalStore,
    private val generateTrainingPlan: GenerateTrainingPlanUseCase,
    private val recalculate: RecalculateFitnessFreshnessUseCase,
    private val updatePredictions: UpdatePerformancePredictionsUseCase,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val snapshots: StateFlow<List<FitnessFreshnessSnapshot>> = trainingLoadRepository
        .observeSnapshotsBetween(LOCAL_USER_ID, today.minusDays(WINDOW_DAYS), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val predictions: StateFlow<List<PerformancePrediction>> = predictionRepository
        .observeForUser(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasGoal: StateFlow<Boolean> = goalStore.goalFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Gợi ý buổi tập hôm nay: theo kế hoạch nếu có, ngược lại theo form (TSB). */
    val dailySuggestion: StateFlow<DailySuggestion?> = combine(
        snapshots,
        goalStore.goalFlow.mapLatest { generateTrainingPlan(today) },
    ) { snaps, plan ->
        val latest = snaps.lastOrNull()
        DailyTrainingSuggestion.suggest(
            tsb = latest?.tsb,
            ctl = latest?.ctl,
            plannedToday = plan?.sessionOn(today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            recalculate(today) // cuộn EWMA tới hôm nay, không đợi job đêm
            updatePredictions() // cửa sổ 90 ngày trượt mỗi ngày
        }
    }
}
