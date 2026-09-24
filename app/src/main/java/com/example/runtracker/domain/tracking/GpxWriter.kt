package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.RoutePoint

/**
 * Xuất GPX 1.1 tối giản (`trk`/`trkseg`/`trkpt` với `ele`/`time`) từ trace đã lưu — không cần thư
 * viện XML ngoài, cấu trúc GPX đơn giản đủ để tự dựng chuỗi. Thuần JVM, có test.
 */
object GpxWriter {

    fun write(activity: Activity, points: List<RoutePoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"RunTracker\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        append("  <trk>\n")
        append("    <name>").append(escapeXml(trackName(activity))).append("</name>\n")
        append("    <trkseg>\n")
        for (point in points) {
            append("      <trkpt lat=\"").append(point.latitude)
                .append("\" lon=\"").append(point.longitude).append("\">\n")
            append("        <ele>").append(point.altitude).append("</ele>\n")
            append("        <time>").append(point.timestamp).append("</time>\n")
            append("      </trkpt>\n")
        }
        append("    </trkseg>\n")
        append("  </trk>\n")
        append("</gpx>\n")
    }

    private fun trackName(activity: Activity): String = "${activity.type.raw} ${activity.startTime}"

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
