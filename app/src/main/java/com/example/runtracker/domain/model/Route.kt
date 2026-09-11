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

/** Một chặng giữa 2 via point liên tiếp trong route đã bám đường qua OSRM. */
data class RouteLeg(
    val polyline: List<GeoPoint>,
    val distanceMeters: Double,
)

/**
 * Kết quả dựng route (từ Directions hoặc fallback đường thẳng).
 *
 * [legs]: chỉ có khi bám đường qua OSRM thành công — mỗi phần tử ứng với 1 chặng giữa 2 via
 * point liên tiếp gửi lên. Dùng để phát hiện chặng nào bị router đi vòng xa (do mạng đường
 * OSM thiếu đoạn nối đúng lúc đó, VD lối ven hồ chưa được vẽ hết) và thay bằng đoạn thẳng
 * theo đúng nét vẽ tay thay vì giữ nguyên đường vòng — xem [BuildRouteUseCase.fromSketch].
 */
data class PlannedRoute(
    val polyline: List<GeoPoint>,
    val distanceMeters: Double,
    val steps: List<RouteStep>,
    val snappedToRoads: Boolean,
    val legs: List<RouteLeg> = emptyList(),
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
