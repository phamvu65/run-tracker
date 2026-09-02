package com.example.runtracker.ui.challenges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeParticipant
import com.example.runtracker.domain.repository.ChallengeRepository
import com.example.runtracker.domain.usecase.UpdateChallengeProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_CHALLENGE_ID = "challengeId"

@HiltViewModel
class ChallengeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val challengeRepository: ChallengeRepository,
    updateChallengeProgress: UpdateChallengeProgressUseCase,
) : ViewModel() {

    private val challengeId: String = checkNotNull(savedStateHandle[ARG_CHALLENGE_ID])

    val challenge: StateFlow<Challenge?> = challengeRepository.observeChallenge(challengeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val participants: StateFlow<List<ChallengeParticipant>> =
        challengeRepository.observeParticipants(challengeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var deleted by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { updateChallengeProgress() }
    }

    fun delete() {
        viewModelScope.launch {
            challengeRepository.deleteChallenge(challengeId)
            deleted = true
        }
    }
}
