package com.example.runtracker.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.tracking.TrackingSession
import com.example.runtracker.tracking.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val LOCAL_USER_ID = "local-user"

@HiltViewModel
class TrackingViewModel @Inject constructor(
    session: TrackingSession,
    repository: ActivityRepository,
) : ViewModel() {

    val tracking: StateFlow<TrackingState> = session.state

    val activities: StateFlow<List<Activity>> = repository.observeActivities(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
