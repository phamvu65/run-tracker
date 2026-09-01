package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.DailyTrainingLoad
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TrainingLoadRepository {

    // Daily training load (nguyên liệu)
    suspend fun upsertDailyLoad(load: DailyTrainingLoad)
    suspend fun getDailyLoad(userId: String, date: LocalDate): DailyTrainingLoad?
    suspend fun getDailyLoadsBetween(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyTrainingLoad>

    // Fitness & Freshness snapshots (CTL/ATL/TSB — Phase 2 job)
    suspend fun upsertSnapshot(snapshot: FitnessFreshnessSnapshot)
    suspend fun getSnapshot(userId: String, date: LocalDate): FitnessFreshnessSnapshot?
    suspend fun getLatestSnapshot(userId: String): FitnessFreshnessSnapshot?
    fun observeSnapshotsBetween(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<FitnessFreshnessSnapshot>>
}
