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
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.time.Duration

/** Tổng hợp số liệu cho phần đầu màn Hồ sơ. */
data class ProfileSummary(
    val activityCount: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalMovingTime: Duration = Duration.ZERO,
    val memberSinceYear: Int = LocalDate.now().year,
    val weekDistanceMeters: Double = 0.0,
    val weekMovingTime: Duration = Duration.ZERO,
    val weekElevationGainMeters: Double = 0.0,
    /** Quãng đường (km) từng tuần, cũ → mới; dài WEEKS phần tử. */
    val weeklyKm: List<Double> = emptyList(),
) {
    companion object {
        const val WEEKS = 12
    }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    activityRepository: ActivityRepository,
) : ViewModel() {

    val user: StateFlow<User?> = userRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val summary: StateFlow<ProfileSummary> = activityRepository.observeActivities(LOCAL_USER_ID)
        .map { activities -> buildSummary(activities) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileSummary())

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
        val ZONE: ZoneId = ZoneId.systemDefault()

        fun buildSummary(activities: List<Activity>): ProfileSummary {
            if (activities.isEmpty()) return ProfileSummary()

            val today = LocalDate.now(ZONE)
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val firstWeekStart = weekStart.minusWeeks((ProfileSummary.WEEKS - 1).toLong())

            var weekDist = 0.0
            var weekMoving = Duration.ZERO
            var weekElev = 0.0
            var totalDist = 0.0
            var totalMoving = Duration.ZERO
            val perWeek = DoubleArray(ProfileSummary.WEEKS)
            var earliestYear = today.year

            activities.forEach { a ->
                totalDist += a.distanceMeters
                totalMoving += a.movingTime
                val date = a.startTime.atZone(ZONE).toLocalDate()
                if (date.year < earliestYear) earliestYear = date.year

                if (!date.isBefore(weekStart)) {
                    weekDist += a.distanceMeters
                    weekMoving += a.movingTime
                    weekElev += a.elevationGainMeters
                }
                if (!date.isBefore(firstWeekStart)) {
                    val idx = java.time.temporal.ChronoUnit.WEEKS.between(firstWeekStart, date).toInt()
                    if (idx in 0 until ProfileSummary.WEEKS) perWeek[idx] += a.distanceMeters / 1000.0
                }
            }

            return ProfileSummary(
                activityCount = activities.size,
                totalDistanceMeters = totalDist,
                totalMovingTime = totalMoving,
                memberSinceYear = earliestYear,
                weekDistanceMeters = weekDist,
                weekMovingTime = weekMoving,
                weekElevationGainMeters = weekElev,
                weeklyKm = perWeek.toList(),
            )
        }
    }
}
