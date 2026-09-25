package com.example.runtracker.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.ActivityRecord
import com.example.runtracker.domain.model.BestEffort
import com.example.runtracker.domain.model.UnlockedBadge
import com.example.runtracker.domain.repository.ActivityRecordRepository
import com.example.runtracker.domain.repository.BadgeRepository
import com.example.runtracker.domain.repository.BestEffortRepository
import com.example.runtracker.domain.training.ActivityRecordType
import com.example.runtracker.domain.training.EffortDistance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

/** Một dòng hiển thị trên màn Thành tích — bọc chung [BestEffort] (theo cự ly) và [ActivityRecord] (toàn buổi). */
sealed interface RecordEntry {
    val activityId: String
    val achievedAt: Instant
    val rank: Int

    data class Distance(val effort: BestEffort) : RecordEntry {
        override val activityId get() = effort.activityId
        override val achievedAt get() = effort.achievedAt
        override val rank get() = effort.rank
    }

    data class Whole(val record: ActivityRecord) : RecordEntry {
        override val activityId get() = record.activityId
        override val achievedAt get() = record.achievedAt
        override val rank get() = record.rank
    }
}

@HiltViewModel
class PersonalRecordsViewModel @Inject constructor(
    bestEffortRepository: BestEffortRepository,
    activityRecordRepository: ActivityRecordRepository,
    badgeRepository: BadgeRepository,
) : ViewModel() {

    private val allEfforts: StateFlow<List<BestEffort>> = bestEffortRepository.observeAllForUser(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allRecords: StateFlow<List<ActivityRecord>> = activityRecordRepository.observeAllForUser(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val badges: StateFlow<List<UnlockedBadge>> = badgeRepository.observeForUser(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Kỷ lục HIỆN TẠI mỗi loại = giá trị TỐT NHẤT trong toàn bộ lịch sử (KHÔNG dùng `rank == 1`
     * vì rank đã đông cứng tại thời điểm đạt được, không tự cập nhật về sau).
     */
    val currentRecords: StateFlow<List<RecordEntry>> = combine(allEfforts, allRecords) { efforts, records ->
        val distanceRows = EffortDistance.entries.mapNotNull { distance ->
            efforts.filter { it.distance == distance }.minByOrNull { it.elapsedSeconds }?.let { RecordEntry.Distance(it) }
        }
        val wholeRows = ActivityRecordType.entries.mapNotNull { type ->
            records.filter { it.type == type }
                .let { candidates -> if (type.higherIsBetter) candidates.maxByOrNull { it.value } else candidates.minByOrNull { it.value } }
                ?.let { RecordEntry.Whole(it) }
        }
        distanceRows + wholeRows
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lịch sử mọi lần lọt top-3 mọi thời đại (cả 2 loại), mới nhất trước — chốt tại thời điểm đạt được. */
    val history: StateFlow<List<RecordEntry>> = combine(allEfforts, allRecords) { efforts, records ->
        val distanceRows = efforts.filter { it.rank <= 3 }.map { RecordEntry.Distance(it) }
        val wholeRows = records.filter { it.rank <= 3 }.map { RecordEntry.Whole(it) }
        (distanceRows + wholeRows).sortedByDescending { it.achievedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
