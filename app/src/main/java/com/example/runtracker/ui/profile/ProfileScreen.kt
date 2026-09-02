package com.example.runtracker.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.ui.components.AthleteAvatar
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.components.StatStrip
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenInfo: () -> Unit = {},
    onOpenSegments: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenZones: () -> Unit = {},
    onOpenRoutes: () -> Unit = {},
    onOpenHrSensor: () -> Unit = {},
    onOpenBeacon: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val name = user?.displayName?.takeIf { it.isNotBlank() } ?: "Bạn"

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onOpenInfo) {
                        Icon(Icons.Filled.Settings, contentDescription = "Thông tin cá nhân")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl),
        ) {
            // ---- Đầu hồ sơ ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AthleteAvatar(name, size = 64.dp)
                Spacer(Modifier.width(Spacing.lg))
                Column {
                    StravaLabel("Thành viên từ ${summary.memberSinceYear}")
                    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    user?.let { u ->
                        val bits = buildList {
                            u.birthYear?.let { add("${java.time.Year.now().value - it} tuổi") }
                            when (u.sex) {
                                com.example.runtracker.domain.model.Sex.MALE -> add("Nam")
                                com.example.runtracker.domain.model.Sex.FEMALE -> add("Nữ")
                                null -> {}
                            }
                        }
                        if (bits.isNotEmpty()) {
                            Text(
                                bits.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Box(Modifier.padding(horizontal = Spacing.screen)) {
                StatStrip(
                    listOf(
                        StatCell("Hoạt động", summary.activityCount.toString()),
                        StatCell("Quãng đường", "%.1f km".format(summary.totalDistanceMeters / 1000.0)),
                        StatCell("Thời gian", formatHours(summary.totalMovingTime.inWholeSeconds)),
                    ),
                )
            }

            Spacer(Modifier.height(Spacing.lg))
            OutlinedButton(
                onClick = onOpenInfo,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Chỉnh sửa hồ sơ")
            }

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLow)

            // ---- Tuần này ----
            Column(Modifier.padding(Spacing.screen)) {
                SectionHeader("Tuần này")
                Spacer(Modifier.height(Spacing.sm))
                StatStrip(
                    listOf(
                        StatCell("Quãng đường", "%.1f km".format(summary.weekDistanceMeters / 1000.0)),
                        StatCell("Thời gian", formatHours(summary.weekMovingTime.inWholeSeconds)),
                        StatCell("Độ cao", "${summary.weekElevationGainMeters.roundToInt()} m"),
                    ),
                )
                if (summary.weeklyKm.any { it > 0.0 }) {
                    Spacer(Modifier.height(Spacing.lg))
                    StravaLabel("${ProfileSummary.WEEKS} tuần qua")
                    Spacer(Modifier.height(Spacing.sm))
                    WeeklyChart(
                        weeklyKm = summary.weeklyKm,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                }
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLow)

            // ---- Danh sách mục ----
            ProfileMenuRow(Icons.Filled.Place, "Đoạn (Segments)", onOpenSegments)
            MenuDivider()
            ProfileMenuRow(Icons.Filled.DateRange, "Kế hoạch tập luyện", onOpenPlan)
            MenuDivider()
            ProfileMenuRow(Icons.Filled.Favorite, "Vùng nhịp tim", onOpenZones)
            MenuDivider()
            ProfileMenuRow(Icons.Filled.LocationOn, "Lộ trình đã lưu", onOpenRoutes)
            MenuDivider()
            ProfileMenuRow(Icons.Filled.Build, "Đai nhịp tim (BLE)", onOpenHrSensor)
            MenuDivider()
            ProfileMenuRow(Icons.Filled.Share, "Theo dõi trực tiếp (Beacon)", onOpenBeacon)
        }
    }
}

@Composable
private fun ProfileMenuRow(icon: ImageVector, title: String, onClick: () -> Unit, subtitle: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        Modifier.padding(start = Spacing.screen),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun WeeklyChart(weeklyKm: List<Double>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (weeklyKm.isEmpty()) return@Canvas
        val maxKm = (weeklyKm.max()).coerceAtLeast(1.0)
        val stepX = if (weeklyKm.size > 1) size.width / (weeklyKm.size - 1) else 0f
        fun y(v: Double) = (size.height - (v / maxKm) * size.height).toFloat()

        // lưới ngang
        listOf(0.0, 0.5, 1.0).forEach { f ->
            val gy = (size.height - f * size.height).toFloat()
            drawLine(grid, androidx.compose.ui.geometry.Offset(0f, gy), androidx.compose.ui.geometry.Offset(size.width, gy), strokeWidth = 1f)
        }

        val linePath = Path()
        val fillPath = Path().apply { moveTo(0f, size.height) }
        weeklyKm.forEachIndexed { i, v ->
            val x = i * stepX
            val yy = y(v)
            if (i == 0) linePath.moveTo(x, yy) else linePath.lineTo(x, yy)
            fillPath.lineTo(x, yy)
        }
        fillPath.lineTo((weeklyKm.size - 1) * stepX, size.height)
        fillPath.close()
        drawPath(fillPath, color = fill)
        drawPath(linePath, color = line, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        weeklyKm.forEachIndexed { i, v ->
            drawCircle(line, radius = 4f, center = androidx.compose.ui.geometry.Offset(i * stepX, y(v)))
        }
    }
}

private fun formatHours(totalSeconds: Long): String {
    if (totalSeconds < 3600) return formatClock(totalSeconds)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (m == 0L) "$h giờ" else "${h}g ${m}p"
}
