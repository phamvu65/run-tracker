package com.example.runtracker.data.mapper

import com.example.runtracker.domain.model.Sex
import com.example.runtracker.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserMappersTest {

    @Test
    fun `user survives round trip`() {
        val user = User(
            id = "local-user",
            displayName = "Vũ",
            email = "a@b.com",
            birthYear = 1995,
            weightKg = 62.5,
            restingHeartRate = 48,
            maxHeartRate = 191,
            sex = Sex.FEMALE,
        )

        assertEquals(user, user.toEntity(createdAt = 1L, updatedAt = 2L).toDomain())
    }

    @Test
    fun `unknown or missing sex string maps to null`() {
        val entity = User(
            id = "u", displayName = "x", email = null, birthYear = null, weightKg = null,
            restingHeartRate = null, maxHeartRate = null, sex = null,
        ).toEntity(createdAt = 0L, updatedAt = 0L)

        assertNull(entity.toDomain().sex)
        assertNull(entity.copy(sex = "ROBOT").toDomain().sex)
    }
}
