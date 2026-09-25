package com.example.runtracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.training.PeriodBucket
import com.example.runtracker.domain.training.PeriodComparison
import com.example.runtracker.domain.training.ProgressStats
import com.example.runtracker.domain.training.TrendPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    activityRepository: ActivityRepository,
) : ViewModel() {

    private val activities: StateFlow<List<Activity>> = activityRepository.observeActivities(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _period = MutableStateFlow(TrendPeriod.WEEK)
    val period: StateFlow<TrendPeriod> = _period.asStateFlow()

    fun selectPeriod(value: TrendPeriod) {
        _period.value = value
    }

    val buckets: StateFlow<List<PeriodBucket>> = combine(activities, period) { acts, p ->
        ProgressStats.buckets(acts, p, count = BUCKET_COUNT.getValue(p))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val comparison: StateFlow<PeriodComparison?> = combine(activities, period) { acts, p ->
        val (current, previous) = rangesFor(p, LocalDate.now())
        ProgressStats.compare(acts, current, previous)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private companion object {
        val BUCKET_COUNT = mapOf(TrendPeriod.WEEK to 8, TrendPeriod.MONTH to 6, TrendPeriod.YEAR to 3)

        /** Khoảng "kỳ này" vs "kỳ liền trước cùng độ dài" — dùng cho [ProgressStats.compare]. */
        fun rangesFor(
            period: TrendPeriod,
            today: LocalDate,
        ): Pair<ClosedRange<LocalDate>, ClosedRange<LocalDate>> = when (period) {
            TrendPeriod.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val prevStart = start.minusWeeks(1)
                (start..start.plusDays(6)) to (prevStart..prevStart.plusDays(6))
            }
            TrendPeriod.MONTH -> {
                val month = YearMonth.from(today)
                val prevMonth = month.minusMonths(1)
                (month.atDay(1)..month.atEndOfMonth()) to (prevMonth.atDay(1)..prevMonth.atEndOfMonth())
            }
            TrendPeriod.YEAR -> {
                (LocalDate.of(today.year, 1, 1)..LocalDate.of(today.year, 12, 31)) to
                    (LocalDate.of(today.year - 1, 1, 1)..LocalDate.of(today.year - 1, 12, 31))
            }
        }
    }
}
