package com.example.runtracker.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.tracking.GeoMath
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer
import org.osmdroid.views.overlay.milestones.MilestonePixelDistanceLister
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Path as AndroidPath

/** Kiểu vẽ mốc: ghim mặc định của osmdroid, hay chấm tròn có chữ (bắt đầu / kết thúc). */
enum class MarkerStyle { PIN, BADGE }

/** Màu mốc xuất phát / về đích — cố ý không lấy từ theme để luôn đọc được trên mọi nền bản đồ. */
val RouteStartColor = Color(0xFF1B8A3A)
val RouteFinishColor = Color(0xFFD32F2F)

/** Đầu và cuối gần nhau hơn mức này thì coi là vòng khép kín, chỉ vẽ một mốc. */
private const val LOOP_ENDPOINT_METERS = 30.0

/**
 * Mốc "bắt đầu" (chấm xanh chữ S) và "kết thúc" (chấm đỏ chữ Đ) của một đường.
 * Vòng khép kín (đầu ≈ cuối) chỉ trả về một mốc chung — hai chấm chồng lên nhau vô nghĩa.
 */
fun startFinishMarkers(points: List<GeoPoint>): List<MapMarker> {
    if (points.size < 2) return emptyList()
    val start = points.first()
    val finish = points.last()
    if (GeoMath.distanceMeters(start, finish) <= LOOP_ENDPOINT_METERS) {
        return listOf(
            MapMarker(start, "Xuất phát & về đích", MarkerStyle.BADGE, RouteStartColor, "S"),
        )
    }
    return listOf(
        MapMarker(start, "Bắt đầu", MarkerStyle.BADGE, RouteStartColor, "S"),
        MapMarker(finish, "Kết thúc", MarkerStyle.BADGE, RouteFinishColor, "Đ"),
    )
}

/** Chấm tròn viền trắng có chữ ở giữa, dùng làm icon [org.osmdroid.views.overlay.Marker]. */
internal fun badgeDrawable(context: Context, fill: Color, label: String?, density: Float): Drawable {
    val size = (28 * density).toInt().coerceAtLeast(8)
    val radius = size / 2f
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(radius, radius, radius, paint)
    paint.color = if (fill == Color.Unspecified) AndroidColor.DKGRAY else fill.toAndroidArgb()
    canvas.drawCircle(radius, radius, radius - 2f * density, paint)

    if (!label.isNullOrBlank()) {
        paint.color = AndroidColor.WHITE
        paint.textSize = 14f * density
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        val metrics = paint.fontMetrics
        canvas.drawText(label, radius, radius - (metrics.ascent + metrics.descent) / 2f, paint)
    }
    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Mũi tên chỉ hướng đi rải đều dọc đường, cách nhau một khoảng cố định *theo pixel* nên
 * mật độ mũi tên không đổi khi phóng to / thu nhỏ. Mũi tên hướng theo chiều vẽ polyline,
 * tức là chiều từ điểm đầu tới điểm cuối của route.
 */
internal fun directionMilestones(lineColor: Color, density: Float): List<MilestoneManager> {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (lineColor.luminance() < 0.55f) AndroidColor.WHITE else AndroidColor.BLACK
        style = Paint.Style.FILL
    }
    val tip = 4.5f * density
    val tail = 2.8f * density
    val half = 3.4f * density
    // Mũi tên hướng theo trục +x; MilestonePathDisplayer sẽ xoay theo hướng đoạn đường.
    val arrow = AndroidPath().apply {
        moveTo(tip, 0f)
        lineTo(-tail, -half)
        lineTo(-tail, half)
        close()
    }
    return listOf(
        MilestoneManager(
            MilestonePixelDistanceLister(ARROW_FIRST_DP * density, ARROW_EVERY_DP * density),
            MilestonePathDisplayer(0.0, true, arrow, paint),
        ),
    )
}

private const val ARROW_FIRST_DP = 34.0
private const val ARROW_EVERY_DP = 62.0

private fun Color.toAndroidArgb(): Int = AndroidColor.argb(
    (alpha * 255f + 0.5f).toInt(),
    (red * 255f + 0.5f).toInt(),
    (green * 255f + 0.5f).toInt(),
    (blue * 255f + 0.5f).toInt(),
)
