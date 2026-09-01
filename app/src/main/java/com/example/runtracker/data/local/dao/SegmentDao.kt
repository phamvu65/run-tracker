package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.SegmentEffortEntity
import com.example.runtracker.data.local.entity.SegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {

    // ---- Segment ----

    @Upsert
    suspend fun upsertSegment(segment: SegmentEntity)

    @Query("SELECT * FROM segments WHERE id = :segmentId")
    suspend fun getSegment(segmentId: String): SegmentEntity?

    @Query("SELECT * FROM segments ORDER BY name ASC")
    fun observeSegments(): Flow<List<SegmentEntity>>

    // ---- SegmentEffort ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffort(effort: SegmentEffortEntity): Long

    @Query("SELECT * FROM segment_efforts WHERE activityId = :activityId")
    suspend fun getEffortsForActivity(activityId: String): List<SegmentEffortEntity>

    /** Leaderboard đầy đủ — không giới hạn top 10 như Strava free. */
    @Query(
        "SELECT * FROM segment_efforts WHERE segmentId = :segmentId " +
            "ORDER BY elapsedSeconds ASC"
    )
    fun observeLeaderboard(segmentId: String): Flow<List<SegmentEffortEntity>>

    @Query(
        "SELECT * FROM segment_efforts WHERE segmentId = :segmentId AND userId = :userId " +
            "ORDER BY elapsedSeconds ASC"
    )
    suspend fun getUserEfforts(segmentId: String, userId: String): List<SegmentEffortEntity>
}
