package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Hồ sơ người dùng. Các trường sinh lý (restingHeartRate, maxHeartRate, birthYear, weightKg)
 * là đầu vào cho công thức TRIMP / training zone / tính calo.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String?,
    val birthYear: Int?,          // tính maxHR ước lượng (220 - age) khi maxHeartRate null
    val weightKg: Double?,        // tính calo
    val restingHeartRate: Int?,   // công thức TRIMP / Banister
    val maxHeartRate: Int?,       // ưu tiên giá trị đo thực tế, override công thức ước lượng
    val sex: String? = null,      // "MALE" / "FEMALE" — chọn hệ số TRIMP; null -> fallback dùng công thức nam
    val createdAt: Long,
    val updatedAt: Long
)
