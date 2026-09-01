package com.example.runtracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DirectionsResponse(
    val status: String,
    val routes: List<RouteDto> = emptyList(),
)

@Serializable
data class RouteDto(
    @SerialName("overview_polyline") val overviewPolyline: PolylineDto,
    val legs: List<LegDto> = emptyList(),
)

@Serializable
data class PolylineDto(val points: String)

@Serializable
data class LegDto(
    val distance: ValueDto,
    val steps: List<StepDto> = emptyList(),
)

@Serializable
data class StepDto(
    @SerialName("html_instructions") val htmlInstructions: String = "",
    val distance: ValueDto,
    @SerialName("start_location") val startLocation: LatLngDto,
)

@Serializable
data class ValueDto(val value: Double)

@Serializable
data class LatLngDto(val lat: Double, val lng: Double)
