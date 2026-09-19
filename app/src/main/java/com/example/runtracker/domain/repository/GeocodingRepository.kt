package com.example.runtracker.domain.repository

interface GeocodingRepository {
    /** Tên khu vực (quận/huyện, tỉnh/thành) tại toạ độ — lỗi mạng/không xác định trả về [Result.failure]. */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<String>
}
