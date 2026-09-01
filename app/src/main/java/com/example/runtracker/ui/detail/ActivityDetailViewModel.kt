package com.example.runtracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.training.TrimpCalculator
import com.example.runtracker.domain.usecase.UpdateDailyTrainingLoadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject

const val ARG_ACTIVITY_ID = "activityId"

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ActivityRepository,
    userRepository: UserRepository,
    private val updateDailyTrainingLoad: UpdateDailyTrainingLoadUseCase,
) : ViewModel() {

    private val activityId: String = checkNotNull(savedStateHandle[ARG_ACTIVITY_ID])

    val state: StateFlow<ActivityDetailUiState> = combine(
        repository.observeActivity(activityId),
        repository.observeRoutePoints(activityId),
        repository.observeLaps(activityId),
        userRepository.observeCurrentUser(),
    ) { activity, points, laps, user ->
        if (activity == null) {
            ActivityDetailUiState.NotFound
        } else {
            ActivityDetailUiState.Loaded(
                activity = activity,
                routePoints = points,
                laps = laps,
                trimp = TrimpCalculator.forActivity(activity, user, Year.now().value),
                canEnterRpe = activity.avgHeartRate == null,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ActivityDetailUiState.Loading,
    )

    fun setPerceivedExertion(rpe: Int) {
        viewModelScope.launch {
            val activity = repository.getActivity(activityId) ?: return@launch
            repository.upsertActivity(activity.copy(perceivedExertion = rpe.coerceIn(1, 10)))
            updateDailyTrainingLoad(activity.startTime.atZone(ZoneId.systemDefault()).toLocalDate())
        }
    }
}

sealed interface ActivityDetailUiState {
    data object Loading : ActivityDetailUiState
    data object NotFound : ActivityDetailUiState
    data class Loaded(
        val activity: Activity,
        val routePoints: List<RoutePoint>,
        val laps: List<ActivityLap>,
        val trimp: Double?,
        val canEnterRpe: Boolean,
    ) : ActivityDetailUiState
}
