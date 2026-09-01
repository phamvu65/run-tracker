package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.PerformancePredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformancePredictionDao {

    @Upsert
    suspend fun upsert(prediction: PerformancePredictionEntity)

    @Upsert
    suspend fun upsertAll(predictions: List<PerformancePredictionEntity>)

    @Query("SELECT * FROM performance_predictions WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<PerformancePredictionEntity>>

    @Query("SELECT * FROM performance_predictions WHERE userId = :userId AND distanceLabel = :distanceLabel")
    suspend fun get(userId: String, distanceLabel: String): PerformancePredictionEntity?
}
