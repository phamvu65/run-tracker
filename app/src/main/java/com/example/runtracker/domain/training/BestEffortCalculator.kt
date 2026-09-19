package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.tracking.RouteStats

/** Cự ly chuẩn để tính "best effort" kiểu Strava — thời gian nhanh nhất chạy liên tục đúng cự ly này. */
enum class EffortDistance(val meters: Double, val label: String) {
    ONE_K(1_000.0, "1 km"),
    FIVE_K(5_000.0, "5 km"),
    TEN_K(10_000.0, "10 km"),
    HALF_MARATHON(21_097.5, "Bán marathon"),
    FULL_MARATHON(42_195.0, "Marathon"),
}

data class BestEffortWindow(val distance: EffortDistance, val elapsedSeconds: Long)

/**
 * Tìm đoạn liên tục NHANH NHẤT dài đúng [EffortDistance.meters] trong một buổi tập — khác PR theo
 * tổng quãng đường: một buổi 10km vẫn có thể có "5km nhanh nhất" nằm ở đoạn giữa chứ không nhất
 * thiết là 5km đầu tiên. Thuần JVM, có test.
 *
 * **Giới hạn đã biết**: không loại trừ thời gian tạm dừng — nếu người dùng bấm Tạm dừng giữa
 * buổi, khoảng thời gian đó vẫn tính vào bất kỳ cửa sổ trượt nào đi ngang qua nó (route point
 * không có điểm nào trong lúc tạm dừng nên không phát hiện được khoảng lặng này). Chấp nhận cho
 * bản v1, cùng nhóm giới hạn với các tính năng tính từ trace thô khác trong app.
 */
object BestEffortCalculator {

    fun compute(
        points: List<RoutePoint>,
        distances: List<EffortDistance> = EffortDistance.entries,
    ): List<BestEffortWindow> {
        if (points.size < 2) return emptyList()
        val cumDist = RouteStats.cumulativeDistances(points)
        val totalDistance = cumDist.last()
        val times = points.map { it.timestamp.toEpochMilli().toDouble() }

        return distances.mapNotNull { distance ->
            if (totalDistance < distance.meters) return@mapNotNull null
            val elapsedMs = bestWindowMs(cumDist, times, distance.meters) ?: return@mapNotNull null
            BestEffortWindow(distance, (elapsedMs / 1_000.0).toLong())
        }
    }

    /**
     * Two-pointer O(n): với mỗi điểm bắt đầu i, đẩy j tới điểm đầu tiên đủ [targetMeters] rồi nội
     * suy thời điểm cắt chính xác trong đoạn [j-1, j]; lấy khoảng thời gian nhỏ nhất trong mọi i.
     * `j` chỉ tăng dần qua các vòng lặp `i` (cumDist tăng dần) nên tổng độ phức tạp vẫn tuyến tính.
     */
    private fun bestWindowMs(cumDist: List<Double>, times: List<Double>, targetMeters: Double): Double? {
        var best: Double? = null
        var j = 0
        val n = cumDist.size
        for (i in 0 until n) {
            if (j < i) j = i
            while (j < n - 1 && cumDist[j] < cumDist[i] + targetMeters) j++
            if (cumDist[j] - cumDist[i] < targetMeters) break
            val endTime = interpolateTimeAt(cumDist, times, j, cumDist[i] + targetMeters)
            val elapsed = endTime - times[i]
            if (best == null || elapsed < best) best = elapsed
        }
        return best
    }

    private fun interpolateTimeAt(cumDist: List<Double>, times: List<Double>, j: Int, targetCum: Double): Double {
        if (j == 0) return times[0]
        val prevDist = cumDist[j - 1]
        val segDist = cumDist[j] - prevDist
        if (segDist <= 0.0) return times[j]
        val fraction = ((targetCum - prevDist) / segDist).coerceIn(0.0, 1.0)
        return times[j - 1] + fraction * (times[j] - times[j - 1])
    }
}
