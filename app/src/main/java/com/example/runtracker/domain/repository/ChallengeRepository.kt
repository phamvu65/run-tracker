package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeParticipant
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {

    fun observeChallenges(): Flow<List<Challenge>>
    fun observeChallenge(id: String): Flow<Challenge?>
    suspend fun getChallenge(id: String): Challenge?
    suspend fun upsertChallenge(challenge: Challenge)
    suspend fun deleteChallenge(id: String)

    /** Participant đã sắp theo tiến độ giảm dần (leaderboard). */
    fun observeParticipants(challengeId: String): Flow<List<ChallengeParticipant>>
    fun observeParticipationsForUser(userId: String): Flow<List<ChallengeParticipant>>
    suspend fun getParticipationsForUser(userId: String): List<ChallengeParticipant>
    suspend fun getParticipant(challengeId: String, userId: String): ChallengeParticipant?
    suspend fun upsertParticipant(participant: ChallengeParticipant)
    suspend fun leaveChallenge(challengeId: String, userId: String)
}
