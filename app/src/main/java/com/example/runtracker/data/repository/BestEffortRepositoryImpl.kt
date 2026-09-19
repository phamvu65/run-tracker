package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.BestEffortDao
import com.example.runtracker.data.local.entity.BestEffortEntity
import com.example.runtracker.domain.model.BestEffort
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.BestEffortRepository
import com.example.runtracker.domain.training.BestEffortCalculator
import com.example.runtracker.domain.training.EffortDistance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BestEffortRepositoryImpl @Inject constructor(
    private val dao: BestEffortDao,
) : BestEffortRepository {

    override suspend fun recomputeForActivity(
        userId: String,
        activityId: String,
        points: List<RoutePoint>,
        achievedAt: Instant,
    ): List<BestEffort> {
        dao.deleteForActivity(activityId)
        val windows = BestEffortCalculator.compute(points)
        if (windows.isEmpty()) return emptyList()

        val results = mutableListOf<BestEffort>()
        val entities = mutableListOf<BestEffortEntity>()
        for (window in windows) {
            // Đã xoá dòng cũ của activity này ở trên nên danh sách này chỉ gồm CÁC BUỔI KHÁC.
            val existing = dao.getRanked(userId, window.distance.name)
            val insertIndex = existing.indexOfFirst { it.elapsedSeconds > window.elapsedSeconds }
                .let { if (it == -1) existing.size else it }
            val rank = insertIndex + 1
            // Buổi đang nằm đúng chỗ ta sắp chen vào — nó sẽ bị đẩy xuống 1 hạng.
            val improved = existing.getOrNull(insertIndex)?.let { it.elapsedSeconds - window.elapsedSeconds }

            entities += BestEffortEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                activityId = activityId,
                distance = window.distance.name,
                elapsedSeconds = window.elapsedSeconds,
                achievedAt = achievedAt.toEpochMilli(),
                rankAtAchievement = rank,
                improvedBySecondsAtAchievement = improved,
            )
            results += BestEffort(window.distance, window.elapsedSeconds, rank, improved)
        }
        dao.insertAll(entities)
        return results
    }

    override suspend fun recomputeAllForUser(
        userId: String,
        activities: List<Pair<String, List<RoutePoint>>>,
        achievedAtByActivity: Map<String, Instant>,
    ) {
        dao.deleteAllForUser(userId)
        for ((activityId, points) in activities) {
            val achievedAt = achievedAtByActivity[activityId] ?: continue
            recomputeForActivity(userId, activityId, points, achievedAt)
        }
    }

    override fun observeGroupedByActivity(userId: String): Flow<Map<String, List<BestEffort>>> =
        dao.observeForUser(userId).map { list ->
            list.groupBy(
                keySelector = { it.activityId },
                valueTransform = {
                    BestEffort(
                        distance = EffortDistance.valueOf(it.distance),
                        elapsedSeconds = it.elapsedSeconds,
                        rank = it.rankAtAchievement,
                        improvedBySeconds = it.improvedBySecondsAtAchievement,
                    )
                },
            )
        }
}
