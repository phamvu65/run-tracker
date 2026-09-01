package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.UserEntity
import com.example.runtracker.domain.model.Sex
import com.example.runtracker.domain.model.User

fun UserEntity.toDomain(): User = User(
    id = id,
    displayName = displayName,
    email = email,
    birthYear = birthYear,
    weightKg = weightKg,
    restingHeartRate = restingHeartRate,
    maxHeartRate = maxHeartRate,
    sex = sex?.let { raw -> Sex.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } },
)

fun User.toEntity(createdAt: Long, updatedAt: Long): UserEntity = UserEntity(
    id = id,
    displayName = displayName,
    email = email,
    birthYear = birthYear,
    weightKg = weightKg,
    restingHeartRate = restingHeartRate,
    maxHeartRate = maxHeartRate,
    sex = sex?.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
