package com.example.runtracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Sex
import com.example.runtracker.domain.model.User
import com.example.runtracker.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    val user: StateFlow<User?> = userRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}
