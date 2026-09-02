package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.ChallengeEntity
import com.example.runtracker.data.local.entity.ChallengeParticipantEntity
import kotlinx.coroutines.flow.Flow

/**
 * Phase 3 — thử thách nhóm quy mô nhỏ.
 */
@Dao
interface ChallengeDao {

    @Upsert
    suspend fun upsertChallenge(challenge: ChallengeEntity)

    @Query("SELECT * FROM challenges ORDER BY startDate DESC")
    fun observeChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    fun observeChallenge(challengeId: String): Flow<ChallengeEntity?>

    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    suspend fun getChallenge(challengeId: String): ChallengeEntity?

    @Query("DELETE FROM challenges WHERE id = :challengeId")
    suspend fun deleteChallengeRow(challengeId: String)

    @Query("DELETE FROM challenge_participants WHERE challengeId = :challengeId")
    suspend fun deleteParticipantsForChallenge(challengeId: String)

    /** Không có FK giữa participant và challenge nên phải tự dọn hàng con. */
    @Transaction
    suspend fun deleteChallenge(challengeId: String) {
        deleteParticipantsForChallenge(challengeId)
        deleteChallengeRow(challengeId)
    }

    @Upsert
    suspend fun upsertParticipant(participant: ChallengeParticipantEntity)

    @Query(
        "SELECT * FROM challenge_participants WHERE challengeId = :challengeId " +
            "ORDER BY currentProgress DESC"
    )
    fun observeParticipants(challengeId: String): Flow<List<ChallengeParticipantEntity>>

    @Query("SELECT * FROM challenge_participants WHERE userId = :userId")
    fun observeParticipationsForUser(userId: String): Flow<List<ChallengeParticipantEntity>>

    @Query("SELECT * FROM challenge_participants WHERE userId = :userId")
    suspend fun getParticipationsForUser(userId: String): List<ChallengeParticipantEntity>

    @Query(
        "SELECT * FROM challenge_participants WHERE challengeId = :challengeId AND userId = :userId"
    )
    suspend fun getParticipant(challengeId: String, userId: String): ChallengeParticipantEntity?

    @Query("DELETE FROM challenge_participants WHERE challengeId = :challengeId AND userId = :userId")
    suspend fun deleteParticipant(challengeId: String, userId: String)
}
