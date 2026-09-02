package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.ChallengeDao
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toEntity
import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeParticipant
import com.example.runtracker.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChallengeRepositoryImpl @Inject constructor(
    private val dao: ChallengeDao,
) : ChallengeRepository {

    override fun observeChallenges(): Flow<List<Challenge>> =
        dao.observeChallenges().map { list -> list.map { it.toDomain() } }

    override fun observeChallenge(id: String): Flow<Challenge?> =
        dao.observeChallenge(id).map { it?.toDomain() }

    override suspend fun getChallenge(id: String): Challenge? = dao.getChallenge(id)?.toDomain()

    override suspend fun upsertChallenge(challenge: Challenge) =
        dao.upsertChallenge(challenge.toEntity())

    override suspend fun deleteChallenge(id: String) = dao.deleteChallenge(id)

    override fun observeParticipants(challengeId: String): Flow<List<ChallengeParticipant>> =
        dao.observeParticipants(challengeId).map { list -> list.map { it.toDomain() } }

    override fun observeParticipationsForUser(userId: String): Flow<List<ChallengeParticipant>> =
        dao.observeParticipationsForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getParticipationsForUser(userId: String): List<ChallengeParticipant> =
        dao.getParticipationsForUser(userId).map { it.toDomain() }

    override suspend fun getParticipant(challengeId: String, userId: String): ChallengeParticipant? =
        dao.getParticipant(challengeId, userId)?.toDomain()

    override suspend fun upsertParticipant(participant: ChallengeParticipant) =
        dao.upsertParticipant(participant.toEntity())

    override suspend fun leaveChallenge(challengeId: String, userId: String) =
        dao.deleteParticipant(challengeId, userId)
}
