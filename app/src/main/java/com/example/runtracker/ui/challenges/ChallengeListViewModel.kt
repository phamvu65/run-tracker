package com.example.runtracker.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.ChallengeStanding
import com.example.runtracker.domain.repository.ChallengeRepository
import com.example.runtracker.domain.usecase.UpdateChallengeProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeListViewModel @Inject constructor(
    challengeRepository: ChallengeRepository,
    updateChallengeProgress: UpdateChallengeProgressUseCase,
) : ViewModel() {

    val standings: StateFlow<List<ChallengeStanding>> = combine(
        challengeRepository.observeChallenges(),
        challengeRepository.observeParticipationsForUser(LOCAL_USER_ID),
    ) { challenges, participations ->
        val progressById = participations.associate { it.challengeId to it.currentProgress }
        challenges.map { ChallengeStanding(it, progressById[it.id] ?: 0.0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { updateChallengeProgress() }
    }
}
