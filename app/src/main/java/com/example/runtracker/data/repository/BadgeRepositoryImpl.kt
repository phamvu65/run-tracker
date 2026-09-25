package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.BadgeDao
import com.example.runtracker.data.local.entity.UnlockedBadgeEntity
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.BadgeType
import com.example.runtracker.domain.model.UnlockedBadge
import com.example.runtracker.domain.repository.BadgeRepository
import com.example.runtracker.domain.training.BadgeEvaluator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepositoryImpl @Inject constructor(
    private val dao: BadgeDao,
) : BadgeRepository {

    override suspend fun detectNew(userId: String, activitiesUpToNow: List<Activity>): List<BadgeType> {
        val latest = activitiesUpToNow.lastOrNull() ?: return emptyList()
        val alreadyUnlocked = dao.getUnlockedTypes(userId)
            .mapNotNull { runCatching { BadgeType.valueOf(it) }.getOrNull() }
            .toSet()
        val newly = BadgeEvaluator.evaluateForLatest(activitiesUpToNow, alreadyUnlocked)
        newly.forEach { type ->
            dao.insert(
                UnlockedBadgeEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    badgeType = type.name,
                    activityId = latest.id,
                    unlockedAt = latest.startTime.toEpochMilli(),
                ),
            )
        }
        return newly
    }

    override suspend fun recomputeAllForUser(userId: String, activities: List<Activity>) {
        dao.deleteAllForUser(userId)
        for (i in activities.indices) {
            detectNew(userId, activities.subList(0, i + 1))
        }
    }

    override fun observeForUser(userId: String): Flow<List<UnlockedBadge>> =
        dao.observeForUser(userId).map { list -> list.map { it.toDomain() } }

    private fun UnlockedBadgeEntity.toDomain() = UnlockedBadge(
        userId = userId,
        type = BadgeType.valueOf(badgeType),
        activityId = activityId,
        unlockedAt = Instant.ofEpochMilli(unlockedAt),
    )
}
