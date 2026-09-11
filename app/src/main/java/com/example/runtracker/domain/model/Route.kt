package com.example.runtracker.domain.model

/**
 * Chế độ di chuyển cho routing. Dùng OSRM của FOSSGIS (`routing.openstreetmap.de`) —
 * miễn phí, không cần key. [osrmHost] là nhánh máy chủ theo hồ sơ, [osrmProfile] là
 * tên profile trong đường dẫn.
 */
enum class TravelMode(val osrmHost: String, val osrmProfile: String) {
    WALKING("routed-foot", "foot"),
    CYCLING("routed-bike", "bike"),
}

/** Một bước rẽ trong route. */
data class RouteStep(
    val instruction: String,
    val location: GeoPoint,
    val distanceMeters: Double,
)

/** Kết quả dựng route (từ Directions hoặc fallback đường thẳng). */
data class PlannedRoute(
    val polyline: List<GeoPoint>,
    val distanceMeters: Double,
    val steps: List<RouteStep>,
    val snappedToRoads: Boolean,
)

/** Route đã lưu. */
data class Route(
    val id: String,
    val userId: String,
    val name: String,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val polyline: List<GeoPoint>,
    val isPublic: Boolean,
    val createdAt: Long,
    val waypoints: List<RouteWaypoint>,
)

data class RouteWaypoint(
    val orderIndex: Int,
    val location: GeoPoint,
    val instruction: String?,
)
