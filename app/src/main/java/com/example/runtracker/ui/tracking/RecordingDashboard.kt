package com.example.runtracker.ui.tracking

import com.example.runtracker.ui.icons.AthleticIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.verticalScroll
import com.example.runtracker.ui.components.PremiumButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.runtracker.core.FeatureFlags
import com.example.runtracker.core.formatClock
import com.example.runtracker.tracking.TrackingState
import com.example.runtracker.tracking.TrackingStatus
import com.example.runtracker.ui.components.DistanceHero
import com.example.runtracker.ui.theme.ambientGlow
import com.example.runtracker.ui.theme.runningPulseAlpha
import com.example.runtracker.ui.theme.RunTrackerTheme
import com.example.runtracker.ui.theme.Spacing
import com.example.runtracker.ui.theme.AthleticType
import com.example.runtracker.ui.theme.formatPaceUnit
import com.example.runtracker.ui.theme.formatDistanceUnit
import com.example.runtracker.ui.theme.formatSpeedUnit

/** High-contrast recording view. All values and actions come from the tracking session. */
@Composable
internal fun RecordingDashboard(
    state: TrackingState,
    sportName: String,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onShowMap: () -> Unit,
    modifier: Modifier = Modifier,
    voiceEnabled: Boolean = true,
    onToggleVoice: () -> Unit = {},
    beaconSharing: Boolean = false,
    beaconCode: String? = null,
    onToggleBeacon: () -> Unit = {},
    showSpeed: Boolean = false,
) {
    val paused = state.status == TrackingStatus.PAUSED
    val pulse = runningPulseAlpha(!paused)
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                sportName.uppercase(), Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = onShowMap, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)) {
                Icon(AthleticIcons.Map, contentDescription = null)
                Spacer(Modifier.width(Spacing.xs))
                Text("Bản đồ")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            val speedKmh = if (state.movingTimeSeconds > 0) state.distanceMeters / state.movingTimeSeconds * 3.6 else 0.0
            RecordingMetric(
                if (showSpeed) "Tốc độ" else "Nhịp độ",
                if (showSpeed) formatSpeedUnit(speedKmh) else formatPaceUnit(state.avgPaceSecPerKm),
                Modifier.weight(1f),
            )
            RecordingMetric("Thời gian", formatClock(state.elapsedSeconds), Modifier.weight(1f))
        }
        if (!paused && (state.pointCount == 0 || !state.gpsSignalOk)) {
            Text("Đang chờ tín hiệu GPS…", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        DistanceHero(state.distanceMeters, modifier = Modifier.ambientGlow(MaterialTheme.colorScheme.primary, 0.12f),
            color = MaterialTheme.colorScheme.primary, centered = true,
            unitColor = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = pulse)))
            Text(
                when {
                    state.autoPaused && paused -> "TỰ TẠM DỪNG · DI CHUYỂN ĐỂ TIẾP TỤC"
                    paused -> "ĐÃ TẠM DỪNG"
                    else -> "ĐANG GHI HOẠT ĐỘNG"
                },
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        if (state.navRouteName != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(state.navRouteName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    state.navDistanceMeters?.let {
                        Text(formatDistanceUnit(it), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                    if (state.navStepCount > 0) {
                        Text("Bước ${state.navStepIndex + 1}/${state.navStepCount}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        when {
                            state.navRerouting -> "Đang tính đường quay lại lộ trình…"
                            state.navOffRoute -> "Bạn đang đi chệch lộ trình"
                            else -> state.navInstruction ?: "Đang theo lộ trình đã chọn"
                        },
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (state.navRerouting) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = onToggleVoice, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)) {
                        Text(if (voiceEnabled) "Tắt đọc chỉ đường" else "Bật đọc chỉ đường")
                    }
                }
            }
        }
        if (state.liveCadenceSpm != null || (FeatureFlags.HEART_RATE_INTEGRATION && state.liveHeartRateBpm != null)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (FeatureFlags.HEART_RATE_INTEGRATION) {
                    state.liveHeartRateBpm?.let { RecordingMetric("Nhịp tim", "$it bpm", Modifier.weight(1f)) }
                }
                state.liveCadenceSpm?.let { RecordingMetric("Cadence", "$it spm", Modifier.weight(1f)) }
            }
        }
        PremiumButton(
            onClick = onPauseResume, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                if (paused) AthleticIcons.Play else AthleticIcons.Pause,
                contentDescription = if (paused) "Tiếp tục ghi" else "Tạm dừng",
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(if (paused) "TIẾP TỤC" else "TẠM DỪNG")
        }
        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp)) {
            Icon(AthleticIcons.Stop, contentDescription = null)
            Spacer(Modifier.width(Spacing.sm))
            Text("Kết thúc buổi tập", fontWeight = FontWeight.Bold)
        }
        if (FeatureFlags.BEACON_LOCATION_SHARING) {
            TextButton(onClick = onToggleBeacon, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)) {
                Text(if (beaconSharing) "Đang chia sẻ · ${beaconCode.orEmpty()}" else "Chia sẻ vị trí trực tiếp")
            }
        }
    }
}

@Composable
private fun RecordingMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val number = value.substringBeforeLast(' ', value)
    val unit = if (' ' in value) value.substringAfterLast(' ') else ""
    BoxWithConstraints(modifier) {
        val fontSize = minOf(40f, maxWidth.value / (number.length.coerceAtLeast(1) * 0.54f * LocalDensity.current.fontScale)).sp
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number, style = AthleticType.metric, fontSize = fontSize, lineHeight = fontSize * 1.15f,
                color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, maxLines = 1)
            if (unit.isNotEmpty()) Text(unit.uppercase(), style = AthleticType.unit,
                color = MaterialTheme.colorScheme.primary)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun RecordingDashboardPreview() {
    RunTrackerTheme {
        RecordingDashboard(
            state = TrackingState(status = TrackingStatus.TRACKING, distanceMeters = 4750.0,
                elapsedSeconds = 1740, movingTimeSeconds = 1740, pointCount = 200),
            sportName = "Chạy bộ", onPauseResume = {}, onStop = {}, onShowMap = {},
        )
    }
}
