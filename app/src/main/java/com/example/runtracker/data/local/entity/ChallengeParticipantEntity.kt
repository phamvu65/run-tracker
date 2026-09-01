package com.example.runtracker.data.local.entity

import androidx.room.Entity

/**
 * Phase 3 — khung sẵn, chưa cần code logic ngay.
 */
@Entity(
    tableName = "challenge_participants",
    primaryKeys = ["challengeId", "userId"]
)
data class ChallengeParticipantEntity(
    val challengeId: String,
    val userId: String,
    val currentProgress: Double,
    val joinedAt: Long
)
