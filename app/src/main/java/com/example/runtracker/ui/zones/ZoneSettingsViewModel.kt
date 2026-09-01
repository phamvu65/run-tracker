package com.example.runtracker.ui.zones

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.maxHeartRateOrEstimate
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.repository.ZoneSettingsRepository
import com.example.runtracker.domain.training.HeartRateZone
import com.example.runtracker.domain.training.HeartRateZones
import com.example.runtracker.domain.training.ZoneCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class ZoneSettingsViewModel @Inject constructor(
    private val zoneSettingsRepository: ZoneSettingsRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    val zones: StateFlow<HeartRateZones?> = zoneSettingsRepository.observeZones(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _estimatedMaxHr = MutableStateFlow<Int?>(null)
    val estimatedMaxHr: StateFlow<Int?> = _estimatedMaxHr.asStateFlow()

    var saved by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val maxHr = userRepository.getCurrentUser()?.maxHeartRateOrEstimate(Year.now().value)
            _estimatedMaxHr.value = maxHr
            zoneSettingsRepository.ensureZones(LOCAL_USER_ID, maxHr)
        }
    }

    /** Vùng mặc định theo nhịp tim tối đa ước tính (cho nút "Đặt lại"). */
    fun defaultZones(): List<HeartRateZone> =
        ZoneCalculator.defaultZones(_estimatedMaxHr.value ?: ZoneCalculator.FALLBACK_MAX_HR)

    fun save(zones: List<HeartRateZone>, thresholdPaceSecPerKm: Double?) {
        viewModelScope.launch {
            zoneSettingsRepository.saveZones(
                HeartRateZones(LOCAL_USER_ID, zones.sortedBy { it.index }, thresholdPaceSecPerKm, 0),
            )
            saved = true
        }
    }

    fun consumeSaved() {
        saved = false
    }
}
