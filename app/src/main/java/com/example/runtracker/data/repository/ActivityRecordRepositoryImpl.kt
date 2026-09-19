package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.ActivityRecordDao
import com.example.runtracker.data.local.entity.ActivityRecordEntity
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityRecord
import com.example.runtracker.domain.repository.ActivityRecordRepository
import com.example.runtracker.domain.training.ActivityRecordCalculator
import com.example.runtracker.domain.training.ActivityRecordType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecordRepositoryImpl @Inject constructor(
    private val dao: ActivityRecordDao,
) : ActivityRecordRepository {

    override suspend fun recomputeForActivity(
        userId: String,
        activityId: String,
        activity: Activity,
        achievedAt: Instant,
    ): List<ActivityRecord> {
        dao.deleteForActivity(activityId)
        val results = mutableListOf<ActivityRecord>()
        val entities = mutableListOf<ActivityRecordEntity>()
        for (type in ActivityRecordType.entries) {
            val value = ActivityRecordCalculator.valueFor(type, activity) ?: continue
            // Đã xoá dòng cũ của activity này ở trên nên danh sách này chỉ gồm CÁC BUỔI KHÁC.
            val existing = if (type.higherIsBetter) {
                dao.getRankedDescending(userId, type.name)
            } else {
                dao.getRankedAscending(userId, type.name)
            }
            val insertIndex = if (type.higherIsBetter) {
                existing.indexOfFirst { it.value < value }
            } else {
                existing.indexOfFirst { it.value > value }
            }.let { if (it == -1) existing.size else it }
            val rank = insertIndex + 1
            // Buổi đang nằm đúng chỗ ta sắp chen vào — nó sẽ bị đẩy xuống 1 hạng.
            val improved = existing.getOrNull(insertIndex)?.let {
                if (type.higherIsBetter) value - it.value else it.value - value
            }

            entities += ActivityRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                activityId = activityId,
                recordType = type.name,
                value = value,
                achievedAt = achievedAt.toEpochMilli(),
                rankAtAchievement = rank,
                improvedByAtAchievement = improved,
            )
            results += ActivityRecord(activityId, type, value, achievedAt, rank, improved)
        }
        dao.insertAll(entities)
        return results
    }

    override suspend fun recomputeAllForUser(userId: String, activities: List<Activity>) {
        dao.deleteAllForUser(userId)
        for (activity in activities) {
            recomputeForActivity(userId, activity.id, activity, activity.startTime)
        }
    }

    override fun observeAllForUser(userId: String): Flow<List<ActivityRecord>> =
        dao.observeForUser(userId).map { list -> list.map { it.toDomain() } }

    private fun ActivityRecordEntity.toDomain() = ActivityRecord(
        activityId = activityId,
        type = ActivityRecordType.valueOf(recordType),
        value = value,
        achievedAt = Instant.ofEpochMilli(achievedAt),
        rank = rankAtAchievement,
        improvedBy = improvedByAtAchievement,
    )
}
