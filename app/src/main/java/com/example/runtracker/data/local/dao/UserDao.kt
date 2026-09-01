package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrent(): Flow<UserEntity?>
}
