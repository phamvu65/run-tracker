package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.runtracker.data.local.entity.ActivityRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {

    @Query("DELETE FROM activity_records WHERE activityId = :activityId")
    suspend fun deleteForActivity(activityId: String)

    @Query("DELETE FROM activity_records WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Insert
    suspend fun insertAll(entries: List<ActivityRecordEntity>)

    /** Xếp hạng khi "nhỏ hơn là tốt hơn" (VD pace) — nhanh nhất trước. */
    @Query("SELECT * FROM activity_records WHERE userId = :userId AND recordType = :recordType ORDER BY value ASC")
    suspend fun getRankedAscending(userId: String, recordType: String): List<ActivityRecordEntity>

    /** Xếp hạng khi "lớn hơn là tốt hơn" (VD quãng đường, độ cao) — lớn nhất trước. */
    @Query("SELECT * FROM activity_records WHERE userId = :userId AND recordType = :recordType ORDER BY value DESC")
    suspend fun getRankedDescending(userId: String, recordType: String): List<ActivityRecordEntity>

    @Query("SELECT * FROM activity_records WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<ActivityRecordEntity>>
}
