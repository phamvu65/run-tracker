package com.example.runtracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.Sex
import com.example.runtracker.domain.model.User
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.time.Duration

/** Tổng hợp số liệu cho đầu màn Hồ sơ — số liệu tuần/tháng nằm ở [weekStat]/[monthStat] (xem `ProfileStats.kt`). */
data class ProfileSummary(
    val activityCount: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalMovingTime: Duration = Duration.ZERO,
    val memberSinceYear: Int = LocalDate.now().year,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    activityRepository: ActivityRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Đầu tuần hiện tại (Thứ Hai) / tháng hiện tại — tính lại mỗi lần [activities] phát mới, luôn là "bây giờ". */
    private val weekStart: LocalDate
        get() = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val currentMonth: YearMonth
        get() = YearMonth.now(zone)

    val user: StateFlow<User?> = userRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val activities: StateFlow<List<Activity>> = activityRepository.observeActivities(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<ProfileSummary> = activities.map { buildSummary(it, zone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileSummary())

    val weekDaysList: StateFlow<List<DayStat>> = activities.map { weekDays(it, weekStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weekStat: StateFlow<PeriodStat> = activities.map {
        val start = weekStart
        periodStat(
            it,
            range = start..start.plusDays(6),
            previousRange = start.minusDays(7)..start.minusDays(1),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodStat())

    val monthDaysGrid: StateFlow<Map<LocalDate, DayStat>> = activities.map { monthDays(it, currentMonth) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val monthStat: StateFlow<PeriodStat> = activities.map {
        val month = currentMonth
        val prevMonth = month.minusMonths(1)
        periodStat(
            it,
            range = month.atDay(1)..month.atEndOfMonth(),
            previousRange = prevMonth.atDay(1)..prevMonth.atEndOfMonth(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodStat())

    val monthlyTrendKm: StateFlow<List<Double>> = activities.map { monthlyTrend(it, months = TREND_MONTHS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch { userRepository.ensureCurrentUser() }
    }

    fun save(
        displayName: String,
        sex: Sex?,
        birthYear: Int?,
        weightKg: Double?,
        restingHeartRate: Int?,
        maxHeartRate: Int?,
    ) {
        viewModelScope.launch {
            val current = userRepository.getCurrentUser() ?: userRepository.ensureCurrentUser()
            userRepository.saveProfile(
                current.copy(
                    displayName = displayName.ifBlank { "Bạn" },
                    sex = sex,
                    birthYear = birthYear,
                    weightKg = weightKg,
                    restingHeartRate = restingHeartRate,
                    maxHeartRate = maxHeartRate,
                ),
            )
            _saved.value = true
        }
    }

    fun consumeSaved() {
        _saved.value = false
    }

    private companion object {
        const val TREND_MONTHS = 6

        fun buildSummary(activities: List<Activity>, zone: ZoneId): ProfileSummary {
            if (activities.isEmpty()) return ProfileSummary()

            var totalDist = 0.0
            var totalMoving = Duration.ZERO
            var earliestYear = LocalDate.now(zone).year

            activities.forEach { a ->
                totalDist += a.distanceMeters
                totalMoving += a.movingTime
                val year = a.startTime.atZone(zone).toLocalDate().year
                if (year < earliestYear) earliestYear = year
            }

            return ProfileSummary(
                activityCount = activities.size,
                totalDistanceMeters = totalDist,
                totalMovingTime = totalMoving,
                memberSinceYear = earliestYear,
            )
        }
    }
}
