package com.example.runtracker.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ActivityListViewModel @Inject constructor(
    repository: ActivityRepository,
) : ViewModel() {

    val activities: StateFlow<List<Activity>> = repository.observeActivities(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
