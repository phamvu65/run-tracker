package com.example.runtracker.data.remote

import kotlinx.serialization.Serializable

/** Phản hồi OSRM `/route/v1`. */
@Serializable
data class OsrmRouteResponse(
    val code: String = "",
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
data class OsrmRoute(
    /** Polyline mã hoá, độ chính xác 1e-5 (geometries=polyline). */
    val geometry: String = "",
    val distance: Double = 0.0,
    val legs: List<OsrmLeg> = emptyList(),
)

@Serializable
data class OsrmLeg(
    val steps: List<OsrmStep> = emptyList(),
    val distance: Double = 0.0,
)

@Serializable
data class OsrmStep(
    val mode: String = "",
    val name: String = "",
    val distance: Double = 0.0,
    val maneuver: OsrmManeuver = OsrmManeuver(),
    /** Polyline mã hoá của riêng bước này (độ chính xác 1e-5) — dùng để dựng lại hình dạng
     *  từng leg (giữa 2 via point liên tiếp) khi cần đối chiếu với nét vẽ tay. */
    val geometry: String = "",
)

@Serializable
data class OsrmManeuver(
    /** [lon, lat]. */
    val location: List<Double> = emptyList(),
    val type: String = "",
    val modifier: String = "",
)
