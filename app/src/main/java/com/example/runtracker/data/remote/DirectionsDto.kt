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
)

@Serializable
data class OsrmStep(
    val name: String = "",
    val distance: Double = 0.0,
    val maneuver: OsrmManeuver = OsrmManeuver(),
)

@Serializable
data class OsrmManeuver(
    /** [lon, lat]. */
    val location: List<Double> = emptyList(),
    val type: String = "",
    val modifier: String = "",
)
