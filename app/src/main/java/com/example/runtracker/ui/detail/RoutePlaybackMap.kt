package com.example.runtracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.example.runtracker.core.formatClock
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.tracking.GeoMath
import com.example.runtracker.ui.common.MapGuideLineColor
import com.example.runtracker.ui.common.MapLine
import com.example.runtracker.ui.common.MapMarker
import com.example.runtracker.ui.common.MarkerStyle
import com.example.runtracker.ui.common.OsmMap
import com.example.runtracker.ui.common.startFinishMarkers
import com.example.runtracker.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Toàn bộ buổi tập phát lại trong ngần này thời gian, bất kể buổi tập dài bao lâu. */
private const val PLAYBACK_DURATION_MS = 25_000L
private const val PLAYBACK_TICK_MS = 120L
private const val RUNNER_EMOJI = "🏃"

/** Vị trí người chạy + phần đường đã "đi qua" tại một mốc tiến độ (0..1) của buổi phát lại. */
private data class PlaybackFrame(val runner: GeoPoint, val revealed: List<GeoPoint>)

/**
 * Bản đồ route có thể "xem lại" kiểu Strava: một hình người chạy di chuyển dọc theo đường đã
 * ghi (mốc thời gian thật của từng điểm GPS — đoạn chạy nhanh thì icon di chuyển nhanh hơn).
 * Đường đi được "VẼ DẦN" theo người chạy (đoạn chưa tới chỉ hiện mờ làm nền định hướng, đoạn đã
 * qua tô đậm màu chính) thay vì hiện sẵn trọn vẹn ngay từ đầu. CAMERA BÁM THEO vệt đã vẽ (fit
 * bounds có khung tối thiểu — xem [com.example.runtracker.ui.common.followTrail]) nên vừa cảm
 * giác "đuổi theo" người chạy vừa luôn thấy bối cảnh xung quanh, không zoom sát tới mức chỉ thấy
 * vài toà nhà. Toàn bộ buổi được nén lại phát trong [PLAYBACK_DURATION_MS] để xem nhanh, không
 * phụ thuộc thời lượng thật. Dừng/hết phát lại -> camera tự quay về bao trọn route (qua nút "về
 * vị trí" có sẵn của [OsmMap]).
 */
@Composable
fun RoutePlaybackMap(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val totalDurationMs = remember(points) {
        if (points.size >= 2) {
            (points.last().timestamp.toEpochMilli() - points.first().timestamp.toEpochMilli())
                .coerceAtLeast(0)
        } else {
            0L
        }
    }
    val canPlayback = points.size >= 2 && totalDurationMs > 0

    var playing by remember(points) { mutableStateOf(false) }
    var progress by remember(points) { mutableFloatStateOf(0f) }

    LaunchedEffect(playing, points) {
        if (!playing) return@LaunchedEffect
        if (progress >= 1f) progress = 0f
        val startTime = System.currentTimeMillis() - (progress * PLAYBACK_DURATION_MS).toLong()
        while (playing) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / PLAYBACK_DURATION_MS).coerceIn(0f, 1f)
            if (progress >= 1f) {
                playing = false
                break
            }
            delay(PLAYBACK_TICK_MS)
        }
    }

    val geoPoints = remember(points) { points.map { GeoPoint(it.latitude, it.longitude) } }
    val frame = remember(points, progress) {
        if (canPlayback) playbackFrameAt(points, progress) else null
    }
    val primary = MaterialTheme.colorScheme.primary
    val guide = MapGuideLineColor

    Box(modifier.clipToBounds()) {
        OsmMap(
            modifier = Modifier.fillMaxSize(),
            lines = buildList {
                // Nền mờ: toàn bộ route, để luôn định hướng được đường sẽ đi kể cả trước khi bấm
                // phát hoặc phần chưa "vẽ" tới.
                if (geoPoints.size >= 2) add(MapLine(geoPoints, guide, widthDp = 3.5f))
                // Đoạn đã đi qua: tô đậm, chỉ dài dần theo tiến độ phát lại (rỗng lúc chưa phát).
                frame?.revealed?.takeIf { it.size >= 2 }?.let {
                    add(MapLine(it, primary, widthDp = 4.5f, showDirection = true))
                }
            },
            markers = startFinishMarkers(geoPoints) +
                listOfNotNull(frame?.runner?.let { MapMarker(it, style = MarkerStyle.EMOJI, label = RUNNER_EMOJI) }),
            fitToLines = true,
            followTrail = if (playing) frame?.revealed else null,
            controlsPadding = PaddingValues(top = Spacing.sm, end = Spacing.sm, bottom = Spacing.sm),
        )

        if (canPlayback) {
            AnimatedVisibility(
                visible = playing,
                enter = fadeIn(),
                exit = fadeOut(),
                // Không đặt TopStart: trùng góc với nút back nổi mà ActivityDetailScreen chồng
                // lên trên RoutePlaybackMap (cả hai cùng full-size nên cùng hệ toạ độ góc).
                modifier = Modifier.align(Alignment.TopCenter).padding(Spacing.md),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                ) {
                    Text(
                        "${formatClock((progress * totalDurationMs / 1000).toLong())} / " +
                            formatClock(totalDurationMs / 1000),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
                    )
                }
            }

            // Đặt giữa-đáy (không phải góc) — góc phải-đáy đã có 2 nút của OsmMap (đổi kiểu bản
            // đồ + về vị trí), đặt cùng góc sẽ đè lên nhau.
            PlaybackFab(
                playing = playing,
                onClick = { playing = !playing },
                modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.md),
            )
        }
    }
}

/** Nút tròn nổi kiểu Strava thay cho thanh phát lại full-width cũ — không che bản đồ. */
@Composable
private fun PlaybackFab(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(60.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.72f),
        shadowElevation = 6.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (playing) {
                PauseGlyph(Color.White)
            } else {
                PlayGlyph(Color.White)
            }
        }
    }
}

/** Tam giác phát tự vẽ (tránh phụ thuộc icon "PlayArrow" lệch tâm khi phóng to trong nút tròn). */
@Composable
private fun PlayGlyph(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        translate(left = size.width * 0.12f) { drawPath(path, color) }
    }
}

@Composable
private fun PauseGlyph(color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) {
            Box(
                Modifier
                    .size(width = 5.dp, height = 20.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}

/**
 * Vị trí người chạy ứng với [fraction] (0..1) của tổng thời lượng buổi tập (nội suy theo mốc
 * thời gian thật), kèm toàn bộ điểm ĐÃ ĐI QUA tính tới đó (dùng vẽ đoạn đường tô đậm dần).
 */
private fun playbackFrameAt(points: List<RoutePoint>, fraction: Float): PlaybackFrame {
    val start = points.first().timestamp.toEpochMilli()
    val end = points.last().timestamp.toEpochMilli()
    val target = start + ((end - start) * fraction.toDouble()).toLong()

    var i = 1
    while (i < points.size - 1 && points[i].timestamp.toEpochMilli() < target) i++
    val a = points[i - 1]
    val b = points[i]
    val aT = a.timestamp.toEpochMilli()
    val bT = b.timestamp.toEpochMilli()
    val segFraction = if (bT > aT) {
        ((target - aT).toDouble() / (bT - aT)).coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    val runner = GeoMath.interpolate(
        GeoPoint(a.latitude, a.longitude),
        GeoPoint(b.latitude, b.longitude),
        segFraction,
    )

    val revealed = ArrayList<GeoPoint>(i + 1)
    for (idx in 0 until i) revealed.add(GeoPoint(points[idx].latitude, points[idx].longitude))
    revealed.add(runner)

    return PlaybackFrame(runner, revealed)
}
