package com.example.runtracker.domain.model

/** Giới tính sinh học — chọn hệ số công thức TRIMP (nam/nữ). */
enum class Sex { MALE, FEMALE }

data class User(
    val id: String,
    val displayName: String,
    val email: String?,
    val birthYear: Int?,
    val weightKg: Double?,
    val restingHeartRate: Int?,
    val maxHeartRate: Int?,
    val sex: Sex?,
)

/** Tuổi tại năm cho trước; null nếu chưa khai năm sinh. */
fun User.ageIn(year: Int): Int? = birthYear?.let { year - it }

/**
 * Nhịp tim tối đa dùng cho tính toán: ưu tiên giá trị đo thực tế, fallback công thức `220 - tuổi`.
 * null nếu không có cả hai.
 */
fun User.maxHeartRateOrEstimate(year: Int): Int? =
    maxHeartRate ?: ageIn(year)?.let { age -> 220 - age }
