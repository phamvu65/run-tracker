package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.ActivityDao
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toEntity
import com.example.runtracker.di.IoDispatcher
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityDetail
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.tracking.GpsTrackFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val activityDao: ActivityDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ActivityRepository {

    override fun observeActivities(userId: String): Flow<List<Activity>> =
        activityDao.observeActivities(userId).map { list -> list.map { it.toDomain() } }

    override fun observeActivity(activityId: String): Flow<Activity?> =
        activityDao.observeActivity(activityId).map { it?.toDomain() }

    override fun observeRoutePoints(activityId: String): Flow<List<RoutePoint>> =
        activityDao.observeRoutePoints(activityId).map { list -> list.map { it.toDomain() } }

    override fun observeLaps(activityId: String): Flow<List<ActivityLap>> =
        activityDao.observeLaps(activityId).map { list -> list.map { it.toDomain() } }

    override fun observeHeartRateSamples(activityId: String): Flow<List<HeartRateSample>> =
        activityDao.observeHeartRateSamples(activityId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActivity(activityId: String): Activity? =
        activityDao.getActivity(activityId)?.toDomain()

    override suspend fun getRoutePoints(activityId: String): List<RoutePoint> =
        activityDao.getRoutePoints(activityId).map { it.toDomain() }

    override suspend fun getActivityDetail(activityId: String): ActivityDetail? = withContext(io) {
        val activity = activityDao.getActivity(activityId)?.toDomain() ?: return@withContext null
        ActivityDetail(
            activity = activity,
            routePoints = activityDao.getRoutePoints(activityId).map { it.toDomain() },
            heartRateSamples = activityDao.getHeartRateSamples(activityId).map { it.toDomain() },
            laps = activityDao.getLaps(activityId).map { it.toDomain() },
        )
    }

    override suspend fun getActivitiesBetween(
        userId: String,
        from: Instant,
        to: Instant,
    ): List<Activity> =
        activityDao.getActivitiesBetween(userId, from.toEpochMilli(), to.toEpochMilli())
            .map { it.toDomain() }

    override suspend fun getUnsyncedActivities(): List<Activity> =
        activityDao.getUnsynced().map { it.toDomain() }

    override suspend fun upsertActivity(activity: Activity) {
        activityDao.upsertActivity(
            activity.toEntity(updatedAt = System.currentTimeMillis(), isSynced = false),
        )
    }

    override suspend fun appendRoutePoints(
        activityId: String,
        points: List<RoutePoint>,
    ): List<RoutePoint> {
        if (points.isEmpty()) return emptyList()
        val previous = activityDao.getLastRoutePoint(activityId)?.toDomain()
        val clean = GpsTrackFilter.sanitize(previous, points)
        if (clean.isNotEmpty()) {
            activityDao.insertRoutePoints(clean.map { it.toEntity(activityId) })
        }
        return clean
    }

    override suspend fun appendHeartRateSamples(
        activityId: String,
        samples: List<HeartRateSample>,
    ) {
        if (samples.isEmpty()) return
        activityDao.insertHeartRateSamples(samples.map { it.toEntity(activityId) })
    }

    override suspend fun replaceLaps(activityId: String, laps: List<ActivityLap>) {
        activityDao.replaceLaps(activityId, laps.map { it.toEntity(activityId) })
    }

    override suspend fun deleteActivity(activityId: String) {
        // route_points / heart_rate_samples xoá theo CASCADE; activity_laps không có FK nên xoá tay.
        activityDao.deleteLaps(activityId)
        activityDao.deleteActivity(activityId)
    }
}
