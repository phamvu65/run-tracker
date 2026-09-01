package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 3 — khung sẵn, chưa cần code logic ngay.
 */
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goalType: String,      // "TOTAL_DISTANCE", "TOTAL_ACTIVITIES"...
    val goalValue: Double,
    val startDate: String,
    val endDate: String,
    val createdByUserId: String
)
