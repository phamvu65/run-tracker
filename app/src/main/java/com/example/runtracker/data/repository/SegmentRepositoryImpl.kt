package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.SegmentDao
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toEntity
import com.example.runtracker.domain.model.Segment
import com.example.runtracker.domain.model.SegmentEffort
import com.example.runtracker.domain.repository.SegmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SegmentRepositoryImpl @Inject constructor(
    private val dao: SegmentDao,
) : SegmentRepository {

    override fun observeSegments(): Flow<List<Segment>> =
        dao.observeSegments().map { list -> list.map { it.toDomain() } }

    override suspend fun getSegment(id: String): Segment? = dao.getSegment(id)?.toDomain()

    override suspend fun getAllSegments(): List<Segment> = dao.getAllSegments().map { it.toDomain() }

    override suspend fun upsertSegment(segment: Segment) = dao.upsertSegment(segment.toEntity())

    override fun observeLeaderboard(segmentId: String): Flow<List<SegmentEffort>> =
        dao.observeLeaderboard(segmentId).map { list ->
            list.mapIndexed { index, entity -> entity.toDomain(rank = index + 1) }
        }

    override suspend fun getEffortsForActivity(activityId: String): List<SegmentEffort> =
        dao.getEffortsForActivity(activityId).map { it.toDomain() }

    override fun observeEffortsForActivity(activityId: String): Flow<List<SegmentEffort>> =
        dao.observeEffortsForActivity(activityId).map { list -> list.map { it.toDomain() } }

    override suspend fun addEffort(effort: SegmentEffort) {
        dao.insertEffort(effort.toEntity())
    }
}
