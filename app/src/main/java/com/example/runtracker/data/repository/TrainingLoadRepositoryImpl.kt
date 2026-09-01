package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.TrainingLoadDao
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toEntity
import com.example.runtracker.domain.model.DailyTrainingLoad
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import com.example.runtracker.domain.repository.TrainingLoadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingLoadRepositoryImpl @Inject constructor(
    private val dao: TrainingLoadDao,
) : TrainingLoadRepository {

    override suspend fun upsertDailyLoad(load: DailyTrainingLoad) = dao.upsertDailyLoad(load.toEntity())

    override suspend fun getDailyLoad(userId: String, date: LocalDate): DailyTrainingLoad? =
        dao.getDailyLoad(userId, date.toString())?.toDomain()

    override suspend fun getDailyLoadsBetween(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyTrainingLoad> =
        dao.getDailyLoadsBetween(userId, from.toString(), to.toString()).map { it.toDomain() }

    override suspend fun getEarliestDailyLoadDate(userId: String): LocalDate? =
        dao.getEarliestDailyLoadDate(userId)?.let(LocalDate::parse)

    override suspend fun upsertSnapshot(snapshot: FitnessFreshnessSnapshot) =
        dao.upsertSnapshot(snapshot.toEntity())

    override suspend fun getSnapshot(userId: String, date: LocalDate): FitnessFreshnessSnapshot? =
        dao.getSnapshot(userId, date.toString())?.toDomain()

    override suspend fun getLatestSnapshot(userId: String): FitnessFreshnessSnapshot? =
        dao.getLatestSnapshot(userId)?.toDomain()

    override fun observeSnapshotsBetween(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<FitnessFreshnessSnapshot>> =
        dao.observeSnapshotsBetween(userId, from.toString(), to.toString())
            .map { list -> list.map { it.toDomain() } }
}
