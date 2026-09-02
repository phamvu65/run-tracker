package com.example.runtracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body gửi lên OpenRouteService: danh sách `[lon, lat]`. */
@Serializable
data class DirectionsRequest(
    val coordinates: List<List<Double>>,
    val instructions: Boolean = true,
)

@Serializable
data class DirectionsResponse(
    val routes: List<RouteDto> = emptyList(),
)

@Serializable
data class RouteDto(
    val summary: SummaryDto = SummaryDto(),
    /** Polyline mã hoá (độ chính xác 1e-5). */
    val geometry: String = "",
    val segments: List<SegmentDto> = emptyList(),
)

@Serializable
data class SummaryDto(val distance: Double = 0.0)

@Serializable
data class SegmentDto(
    val steps: List<StepDto> = emptyList(),
)

@Serializable
data class StepDto(
    val instruction: String = "",
    val distance: Double = 0.0,
    /** [chỉ số điểm đầu, chỉ số điểm cuối] trong mảng toạ độ của geometry. */
    @SerialName("way_points") val wayPoints: List<Int> = emptyList(),
)
