package com.example.runtracker.ui.complete

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.PersonalRecord
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.usecase.DetectPersonalRecordsUseCase
import com.example.runtracker.ui.detail.ARG_ACTIVITY_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunCompleteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ActivityRepository,
    private val userRepository: UserRepository,
    private val detectPersonalRecords: DetectPersonalRecordsUseCase,
) : ViewModel() {

    val activityId: String = checkNotNull(savedStateHandle[ARG_ACTIVITY_ID])

    /** Cố định cho cả vòng đời màn hình — không đổi câu mỗi lần recompose. */
    val quote: String = MotivationalQuotes.random()

    var activity by mutableStateOf<Activity?>(null)
        private set
    var records by mutableStateOf<List<PersonalRecord>>(emptyList())
        private set
    var athleteName by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            activity = repository.getActivity(activityId)
            records = detectPersonalRecords(activityId)
            athleteName = userRepository.getCurrentUser()?.displayName?.takeIf { it.isNotBlank() }
            loading = false
        }
    }
}
