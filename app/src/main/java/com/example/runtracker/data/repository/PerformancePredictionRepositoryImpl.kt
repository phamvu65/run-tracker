package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.PerformancePredictionDao
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toEntity
import com.example.runtracker.domain.model.PerformancePrediction
import com.example.runtracker.domain.repository.PerformancePredictionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformancePredictionRepositoryImpl @Inject constructor(
    private val dao: PerformancePredictionDao,
) : PerformancePredictionRepository {

    override fun observeForUser(userId: String): Flow<List<PerformancePrediction>> =
        dao.observeForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun get(userId: String, distanceLabel: String): PerformancePrediction? =
        dao.get(userId, distanceLabel)?.toDomain()

    override suspend fun replaceAll(predictions: List<PerformancePrediction>) =
        dao.upsertAll(predictions.map { it.toEntity() })
}
