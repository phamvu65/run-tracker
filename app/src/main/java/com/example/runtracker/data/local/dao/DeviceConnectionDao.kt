package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.DeviceConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceConnectionDao {

    @Upsert
    suspend fun upsert(connection: DeviceConnectionEntity)

    @Query("SELECT * FROM device_connections WHERE userId = :userId")
    fun observeForUser(userId: String): Flow<List<DeviceConnectionEntity>>

    @Query("SELECT * FROM device_connections WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveForUser(userId: String): List<DeviceConnectionEntity>

    @Query("DELETE FROM device_connections WHERE id = :id")
    suspend fun delete(id: String)
}
