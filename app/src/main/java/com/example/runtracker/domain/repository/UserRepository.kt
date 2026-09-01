package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun observeCurrentUser(): Flow<User?>

    suspend fun getCurrentUser(): User?

    /** Trả về user cục bộ, tạo bản ghi mặc định nếu chưa có. */
    suspend fun ensureCurrentUser(): User

    /** Ghi đè hồ sơ (giữ nguyên createdAt, cập nhật updatedAt). */
    suspend fun saveProfile(user: User)
}
