package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.ChallengeEntity
import com.example.runtracker.data.local.entity.ChallengeParticipantEntity
import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.model.ChallengeParticipant
import java.time.Instant
import java.time.LocalDate

fun ChallengeEntity.toDomain(): Challenge = Challenge(
    id = id,
    name = name,
    goalType = ChallengeGoalType.fromRaw(goalType),
    goalValue = goalValue,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    createdByUserId = createdByUserId,
)

fun Challenge.toEntity(): ChallengeEntity = ChallengeEntity(
    id = id,
    name = name,
    goalType = goalType.raw,
    goalValue = goalValue,
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    createdByUserId = createdByUserId,
)

fun ChallengeParticipantEntity.toDomain(): ChallengeParticipant = ChallengeParticipant(
    challengeId = challengeId,
    userId = userId,
    currentProgress = currentProgress,
    joinedAt = Instant.ofEpochMilli(joinedAt),
)

fun ChallengeParticipant.toEntity(): ChallengeParticipantEntity = ChallengeParticipantEntity(
    challengeId = challengeId,
    userId = userId,
    currentProgress = currentProgress,
    joinedAt = joinedAt.toEpochMilli(),
)
