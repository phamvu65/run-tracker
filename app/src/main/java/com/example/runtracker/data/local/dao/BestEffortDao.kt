package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.runtracker.data.local.entity.BestEffortEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BestEffortDao {

    @Query("DELETE FROM best_efforts WHERE activityId = :activityId")
    suspend fun deleteForActivity(activityId: String)

    @Query("DELETE FROM best_efforts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Insert
    suspend fun insertAll(entries: List<BestEffortEntity>)

    /** Toàn bộ dòng của một cự ly, xếp nhanh nhất trước — dùng tính hạng lúc chốt buổi. */
    @Query("SELECT * FROM best_efforts WHERE userId = :userId AND distance = :distance ORDER BY elapsedSeconds ASC")
    suspend fun getRanked(userId: String, distance: String): List<BestEffortEntity>

    /** Mọi best effort của user, dùng gộp theo activityId cho feed hoạt động. */
    @Query("SELECT * FROM best_efforts WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<BestEffortEntity>>
}
