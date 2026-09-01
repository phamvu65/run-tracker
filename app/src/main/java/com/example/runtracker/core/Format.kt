package com.example.runtracker.core

import kotlin.math.roundToInt

/** Giây -> "h:mm:ss" (bỏ giờ nếu < 1h). */
fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/** Pace giây/km -> "m:ss /km"; "--:-- /km" khi chưa có dữ liệu. */
fun formatPace(secPerKm: Double): String {
    if (secPerKm <= 0.0) return "--:-- /km"
    val total = secPerKm.roundToInt()
    return "%d:%02d /km".format(total / 60, total % 60)
}
