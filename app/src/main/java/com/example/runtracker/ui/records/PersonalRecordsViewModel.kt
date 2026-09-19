package com.example.runtracker.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.BestEffort
import com.example.runtracker.domain.repository.BestEffortRepository
import com.example.runtracker.domain.training.EffortDistance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PersonalRecordsViewModel @Inject constructor(
    bestEffortRepository: BestEffortRepository,
) : ViewModel() {

    private val all: StateFlow<List<BestEffort>> = bestEffortRepository.observeAllForUser(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Kỷ lục HIỆN TẠI mỗi cự ly = `elapsedSeconds` nhỏ nhất trong toàn bộ lịch sử (KHÔNG dùng
     * `rank == 1` vì rank đã đông cứng tại thời điểm đạt được, không tự cập nhật về sau).
     */
    val currentRecords: StateFlow<List<BestEffort>> = all
        .map { list ->
            EffortDistance.entries.mapNotNull { distance ->
                list.filter { it.distance == distance }.minByOrNull { it.elapsedSeconds }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lịch sử mọi lần lọt top-3 mọi thời đại, mới nhất trước — mỗi dòng chốt tại thời điểm đạt được. */
    val history: StateFlow<List<BestEffort>> = all
        .map { list -> list.filter { it.rank <= 3 }.sortedByDescending { it.achievedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
