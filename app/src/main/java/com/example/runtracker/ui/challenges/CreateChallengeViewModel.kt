package com.example.runtracker.ui.challenges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.usecase.CreateChallengeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CreateChallengeViewModel @Inject constructor(
    private val createChallenge: CreateChallengeUseCase,
) : ViewModel() {

    var savedId by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun create(
        name: String,
        goalType: ChallengeGoalType,
        inputValue: Double?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) {
        if (saving) return
        if (inputValue == null || inputValue <= 0.0) {
            error = "Nhập mục tiêu lớn hơn 0"
            return
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            error = "Khoảng ngày không hợp lệ"
            return
        }
        error = null
        saving = true
        viewModelScope.launch {
            savedId = createChallenge(
                name = name,
                goalType = goalType,
                goalValue = goalType.toBaseValue(inputValue),
                startDate = startDate,
                endDate = endDate,
            )
            if (savedId == null) error = "Không tạo được thử thách"
            saving = false
        }
    }

    fun consumeSaved() {
        savedId = null
    }
}
