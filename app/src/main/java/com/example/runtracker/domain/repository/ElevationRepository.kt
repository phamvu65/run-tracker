package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.GeoPoint

/** Tra độ cao thật (DEM) cho một danh sách toạ độ — xem `CorrectElevationUseCase`. */
interface ElevationRepository {

    /** Trả về độ cao (m) theo ĐÚNG thứ tự [points]; cùng kích thước khi thành công. */
    suspend fun elevationsFor(points: List<GeoPoint>): Result<List<Double>>
}
