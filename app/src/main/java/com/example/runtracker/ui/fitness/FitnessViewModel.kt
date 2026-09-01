package com.example.runtracker.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.usecase.RecalculateFitnessFreshnessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val WINDOW_DAYS = 90L

@HiltViewModel
class FitnessViewModel @Inject constructor(
    trainingLoadRepository: TrainingLoadRepository,
    private val recalculate: RecalculateFitnessFreshnessUseCase,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val snapshots: StateFlow<List<FitnessFreshnessSnapshot>> = trainingLoadRepository
        .observeSnapshotsBetween(LOCAL_USER_ID, today.minusDays(WINDOW_DAYS), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Cuộn EWMA tới hôm nay ngay khi mở màn (không đợi job đêm) — gồm cả suy giảm ngày nghỉ.
        viewModelScope.launch { recalculate(today) }
    }
}
