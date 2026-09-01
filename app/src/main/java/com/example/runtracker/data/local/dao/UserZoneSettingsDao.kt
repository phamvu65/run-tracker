package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.UserZoneSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserZoneSettingsDao {

    @Upsert
    suspend fun upsert(settings: UserZoneSettingsEntity)

    @Query("SELECT * FROM user_zone_settings WHERE userId = :userId")
    suspend fun getForUser(userId: String): UserZoneSettingsEntity?

    @Query("SELECT * FROM user_zone_settings WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<UserZoneSettingsEntity?>
}
