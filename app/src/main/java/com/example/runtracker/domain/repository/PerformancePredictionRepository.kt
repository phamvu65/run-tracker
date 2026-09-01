package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.PerformancePrediction
import kotlinx.coroutines.flow.Flow

interface PerformancePredictionRepository {

    fun observeForUser(userId: String): Flow<List<PerformancePrediction>>

    suspend fun get(userId: String, distanceLabel: String): PerformancePrediction?

    /** Ghi đè toàn bộ dự đoán (bộ nhãn cố định 5K/10K/HALF/FULL). */
    suspend fun replaceAll(predictions: List<PerformancePrediction>)
}
