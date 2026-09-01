package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.ActivityEntity
import com.example.runtracker.data.local.entity.ActivityLapEntity
import com.example.runtracker.data.local.entity.HeartRateSampleEntity
import com.example.runtracker.data.local.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho aggregate Activity: bản ghi tổng hợp + GPS trace + mẫu HR + lap.
 */
@Dao
interface ActivityDao {

    // ---- Activity ----

    @Upsert
    suspend fun upsertActivity(activity: ActivityEntity)

    @Query("SELECT * FROM activities WHERE id = :activityId")
    suspend fun getActivity(activityId: String): ActivityEntity?

    @Query("SELECT * FROM activities WHERE id = :activityId")
    fun observeActivity(activityId: String): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY startTime DESC")
    fun observeActivities(userId: String): Flow<List<ActivityEntity>>

    @Query(
        "SELECT * FROM activities WHERE userId = :userId " +
            "AND startTime BETWEEN :fromEpochMillis AND :toEpochMillis ORDER BY startTime ASC"
    )
    suspend fun getActivitiesBetween(
        userId: String,
        fromEpochMillis: Long,
        toEpochMillis: Long
    ): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ActivityEntity>

    @Query("DELETE FROM activities WHERE id = :activityId")
    suspend fun deleteActivity(activityId: String)

    // ---- RoutePoint ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutePoint(point: RoutePointEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM route_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    suspend fun getRoutePoints(activityId: String): List<RoutePointEntity>

    /** Điểm mới nhất đã lưu — mốc để lọc nhiễu batch điểm kế tiếp mà không phải đọc cả trace. */
    @Query("SELECT * FROM route_points WHERE activityId = :activityId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastRoutePoint(activityId: String): RoutePointEntity?

    @Query("SELECT * FROM route_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    fun observeRoutePoints(activityId: String): Flow<List<RoutePointEntity>>

    // ---- HeartRateSample ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHeartRateSamples(samples: List<HeartRateSampleEntity>)

    @Query("SELECT * FROM heart_rate_samples WHERE activityId = :activityId ORDER BY timestamp ASC")
    suspend fun getHeartRateSamples(activityId: String): List<HeartRateSampleEntity>

    // ---- ActivityLap ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<ActivityLapEntity>)

    @Query("SELECT * FROM activity_laps WHERE activityId = :activityId ORDER BY lapIndex ASC")
    suspend fun getLaps(activityId: String): List<ActivityLapEntity>

    @Query("SELECT * FROM activity_laps WHERE activityId = :activityId ORDER BY lapIndex ASC")
    fun observeLaps(activityId: String): Flow<List<ActivityLapEntity>>

    @Query("DELETE FROM activity_laps WHERE activityId = :activityId")
    suspend fun deleteLaps(activityId: String)

    @Transaction
    suspend fun replaceLaps(activityId: String, laps: List<ActivityLapEntity>) {
        deleteLaps(activityId)
        insertLaps(laps)
    }
}
