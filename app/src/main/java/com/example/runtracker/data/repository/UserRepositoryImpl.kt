package com.example.runtracker.data.repository

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.data.local.dao.UserDao
import com.example.runtracker.data.local.entity.UserEntity
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toEntity
import com.example.runtracker.domain.model.User
import com.example.runtracker.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
) : UserRepository {

    override fun observeCurrentUser(): Flow<User?> =
        userDao.observeById(LOCAL_USER_ID).map { it?.toDomain() }

    override suspend fun getCurrentUser(): User? = userDao.getById(LOCAL_USER_ID)?.toDomain()

    override suspend fun ensureCurrentUser(): User {
        userDao.getById(LOCAL_USER_ID)?.let { return it.toDomain() }
        val now = System.currentTimeMillis()
        val entity = UserEntity(
            id = LOCAL_USER_ID,
            displayName = "Bạn",
            email = null,
            birthYear = null,
            weightKg = null,
            restingHeartRate = null,
            maxHeartRate = null,
            sex = null,
            createdAt = now,
            updatedAt = now,
        )
        userDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun saveProfile(user: User) {
        val now = System.currentTimeMillis()
        val createdAt = userDao.getById(user.id)?.createdAt ?: now
        userDao.upsert(user.toEntity(createdAt = createdAt, updatedAt = now))
    }
}
