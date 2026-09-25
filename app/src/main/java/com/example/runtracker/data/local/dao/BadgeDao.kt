package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.runtracker.data.local.entity.UnlockedBadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: UnlockedBadgeEntity)

    @Query("SELECT badgeType FROM unlocked_badges WHERE userId = :userId")
    suspend fun getUnlockedTypes(userId: String): List<String>

    @Query("SELECT * FROM unlocked_badges WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun observeForUser(userId: String): Flow<List<UnlockedBadgeEntity>>

    @Query("DELETE FROM unlocked_badges WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
