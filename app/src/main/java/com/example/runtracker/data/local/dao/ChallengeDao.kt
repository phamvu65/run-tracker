package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.ChallengeEntity
import com.example.runtracker.data.local.entity.ChallengeParticipantEntity
import kotlinx.coroutines.flow.Flow

/**
 * Phase 3 — khung sẵn.
 */
@Dao
interface ChallengeDao {

    @Upsert
    suspend fun upsertChallenge(challenge: ChallengeEntity)

    @Query("SELECT * FROM challenges ORDER BY startDate DESC")
    fun observeChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    suspend fun getChallenge(challengeId: String): ChallengeEntity?

    @Upsert
    suspend fun upsertParticipant(participant: ChallengeParticipantEntity)

    @Query("SELECT * FROM challenge_participants WHERE challengeId = :challengeId ORDER BY currentProgress DESC")
    fun observeParticipants(challengeId: String): Flow<List<ChallengeParticipantEntity>>

    @Query("SELECT * FROM challenge_participants WHERE userId = :userId")
    suspend fun getParticipationsForUser(userId: String): List<ChallengeParticipantEntity>
}
