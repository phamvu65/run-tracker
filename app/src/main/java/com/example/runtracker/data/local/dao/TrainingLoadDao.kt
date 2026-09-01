package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.DailyTrainingLoadEntity
import com.example.runtracker.data.local.entity.FitnessFreshnessSnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho pipeline training load: DailyTrainingLoad (nguyên liệu) -> FitnessFreshnessSnapshot (CTL/ATL/TSB).
 */
@Dao
interface TrainingLoadDao {

    // ---- DailyTrainingLoad ----

    @Upsert
    suspend fun upsertDailyLoad(load: DailyTrainingLoadEntity)

    @Query("SELECT * FROM daily_training_load WHERE userId = :userId AND date = :date")
    suspend fun getDailyLoad(userId: String, date: String): DailyTrainingLoadEntity?

    @Query(
        "SELECT * FROM daily_training_load WHERE userId = :userId " +
            "AND date BETWEEN :fromDate AND :toDate ORDER BY date ASC"
    )
    suspend fun getDailyLoadsBetween(
        userId: String,
        fromDate: String,
        toDate: String
    ): List<DailyTrainingLoadEntity>

    // ---- FitnessFreshnessSnapshot ----

    @Upsert
    suspend fun upsertSnapshot(snapshot: FitnessFreshnessSnapshotEntity)

    @Query("SELECT * FROM fitness_freshness_snapshots WHERE userId = :userId AND date = :date")
    suspend fun getSnapshot(userId: String, date: String): FitnessFreshnessSnapshotEntity?

    /** Snapshot mới nhất — điểm bắt đầu cho phép tính đệ quy của ngày kế tiếp. */
    @Query(
        "SELECT * FROM fitness_freshness_snapshots WHERE userId = :userId " +
            "ORDER BY date DESC LIMIT 1"
    )
    suspend fun getLatestSnapshot(userId: String): FitnessFreshnessSnapshotEntity?

    @Query(
        "SELECT * FROM fitness_freshness_snapshots WHERE userId = :userId " +
            "AND date BETWEEN :fromDate AND :toDate ORDER BY date ASC"
    )
    fun observeSnapshotsBetween(
        userId: String,
        fromDate: String,
        toDate: String
    ): Flow<List<FitnessFreshnessSnapshotEntity>>
}
