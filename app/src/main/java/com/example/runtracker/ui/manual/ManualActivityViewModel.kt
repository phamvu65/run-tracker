package com.example.runtracker.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.usecase.CreateManualActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ManualActivityViewModel @Inject constructor(
    private val createManualActivity: CreateManualActivityUseCase,
) : ViewModel() {

    /**
     * @param distanceKm quãng đường (km).
     * @param durationMinutes thời gian (phút).
     * @param perceivedExertion RPE 1-10, null nếu người dùng không nhập.
     * @param onCreated callback với id activity mới, hoặc null nếu dữ liệu không hợp lệ.
     */
    fun save(
        type: ActivityType,
        startTime: Instant,
        distanceKm: Double,
        durationMinutes: Double,
        perceivedExertion: Int?,
        onCreated: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val id = createManualActivity(
                type = type,
                startTime = startTime,
                distanceMeters = distanceKm * 1000.0,
                durationSeconds = (durationMinutes * 60).toLong(),
                perceivedExertion = perceivedExertion,
            )
            onCreated(id)
        }
    }
}
