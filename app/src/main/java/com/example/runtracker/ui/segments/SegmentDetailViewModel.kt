package com.example.runtracker.ui.segments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Segment
import com.example.runtracker.domain.model.SegmentEffort
import com.example.runtracker.domain.repository.SegmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_SEGMENT_ID = "segmentId"

@HiltViewModel
class SegmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val segmentRepository: SegmentRepository,
) : ViewModel() {

    private val segmentId: String = checkNotNull(savedStateHandle[ARG_SEGMENT_ID])

    private val _segment = MutableStateFlow<Segment?>(null)
    val segment: StateFlow<Segment?> = _segment.asStateFlow()

    val leaderboard: StateFlow<List<SegmentEffort>> = segmentRepository.observeLeaderboard(segmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _segment.value = segmentRepository.getSegment(segmentId) }
    }
}
