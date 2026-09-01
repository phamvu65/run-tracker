package com.example.runtracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

const val ARG_ACTIVITY_ID = "activityId"

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ActivityRepository,
) : ViewModel() {

    private val activityId: String = checkNotNull(savedStateHandle[ARG_ACTIVITY_ID])

    val state: StateFlow<ActivityDetailUiState> = combine(
        repository.observeActivity(activityId),
        repository.observeRoutePoints(activityId),
    ) { activity, points ->
        if (activity == null) {
            ActivityDetailUiState.NotFound
        } else {
            ActivityDetailUiState.Loaded(activity, points)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ActivityDetailUiState.Loading,
    )
}

sealed interface ActivityDetailUiState {
    data object Loading : ActivityDetailUiState
    data object NotFound : ActivityDetailUiState
    data class Loaded(
        val activity: Activity,
        val routePoints: List<RoutePoint>,
    ) : ActivityDetailUiState
}
