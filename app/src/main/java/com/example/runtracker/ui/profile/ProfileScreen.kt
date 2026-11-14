package com.example.runtracker.ui.profile

import com.example.runtracker.ui.icons.AthleticIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.FeatureFlags
import com.example.runtracker.core.formatClock
import com.example.runtracker.ui.components.AthleteAvatar
import com.example.runtracker.ui.components.IconStatGrid
import com.example.runtracker.ui.components.IconStatTileData
import com.example.runtracker.ui.components.PeriodSwitch
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing
import com.example.runtracker.ui.theme.formatDistanceUnit
import com.example.runtracker.ui.theme.formatPaceUnit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onOpenInfo: () -> Unit = {},
    onOpenRecords: () -> Unit = {},
    onOpenSegments: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenZones: () -> Unit = {},
    onOpenRoutes: () -> Unit = {},
    onOpenHrSensor: () -> Unit = {},
    onOpenBeacon: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val weekDaysList by viewModel.weekDaysList.collectAsState()
    val weekStat by viewModel.weekStat.collectAsState()
    val monthDaysGrid by viewModel.monthDaysGrid.collectAsState()
    val monthStat by viewModel.monthStat.collectAsState()
    val monthlyTrendKm by viewModel.monthlyTrendKm.collectAsState()
    val name = user?.displayName?.takeIf { it.isNotBlank() } ?: "Bạn"
    var period by remember { mutableStateOf(ChartPeriod.WEEKLY) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            com.example.runtracker.ui.components.CompactTopHeader(title = "Hồ sơ")
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.sm, bottom = Spacing.xl),
        ) {
            // ---- Đầu hồ sơ ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AthleteAvatar(name, size = 48.dp)
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
                ProfileActivityOverview(summary)
            }

            Spacer(Modifier.height(Spacing.lg))
            OutlinedButton(
                onClick = onOpenInfo,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
            ) {
                Icon(AthleticIcons.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Chỉnh sửa hồ sơ")
            }

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLow)

            // ---- Tuần này / Tháng này (kiểu GoRun "Diagram Card Weekly/Monthly") ----
            Spacer(Modifier.height(Spacing.lg))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StravaLabel("Hoạt động")
                PeriodSwitch(
                    options = listOf("Tuần", "Tháng"),
                    selectedIndex = period.ordinal,
                    onSelect = { period = ChartPeriod.entries[it] },
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Box(Modifier.padding(horizontal = Spacing.screen)) {
                if (period == ChartPeriod.WEEKLY) {
                    WeeklyDiagramCard(stat = weekStat, days = weekDaysList)
                } else {
                    MonthlyDiagramCard(
                        month = YearMonth.now(),
                        stat = monthStat,
                        days = monthDaysGrid,
                        trendKm = monthlyTrendKm,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLow)

            // ---- Danh sách mục ----
            ProfileMenuRow(AthleticIcons.PersonalBest, "Thành tích", onOpenRecords)
            MenuDivider()
            ProfileMenuRow(AthleticIcons.RoutePlanning, "Đoạn (Segments)", onOpenSegments)
            MenuDivider()
            ProfileMenuRow(AthleticIcons.History, "Kế hoạch tập luyện", onOpenPlan)
            MenuDivider()
            if (FeatureFlags.HEART_RATE_INTEGRATION) {
                ProfileMenuRow(AthleticIcons.HeartRate, "Vùng nhịp tim", onOpenZones)
                MenuDivider()
            }
            ProfileMenuRow(AthleticIcons.RoutePlanning, "Lộ trình đã lưu", onOpenRoutes)
            MenuDivider()
            if (FeatureFlags.HEART_RATE_INTEGRATION) {
                ProfileMenuRow(AthleticIcons.HeartRate, "Đai nhịp tim (BLE)", onOpenHrSensor)
                MenuDivider()
            }
            if (FeatureFlags.BEACON_LOCATION_SHARING) {
                ProfileMenuRow(AthleticIcons.Share, "Theo dõi trực tiếp (Beacon)", onOpenBeacon)
                MenuDivider()
            }
            ProfileMenuRow(AthleticIcons.Settings, "Cài đặt", onOpenSettings)
        }
    }
}

private enum class ChartPeriod { WEEKLY, MONTHLY }

@Composable
private fun WeeklyDiagramCard(stat: PeriodStat, days: List<DayStat>, modifier: Modifier = Modifier) {
    var selectedIndex by remember { mutableStateOf((LocalDate.now().dayOfWeek.value - 1).coerceIn(0, 6)) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        DeltaRow(deltaPct = stat.deltaPct, comparisonLabel = "tuần trước")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDistanceUnit(stat.distanceMeters),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            PeriodIconBadge()
        }
        if (days.any { it.hasActivity }) {
            WeeklyActivityChart(
                days = days,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ProfileStatGrid(stat)
    }
}

@Composable
private fun MonthlyDiagramCard(
    month: YearMonth,
    stat: PeriodStat,
    days: Map<LocalDate, DayStat>,
    trendKm: List<Double>,
    modifier: Modifier = Modifier,
) {
    var selectedDate by remember(month) { mutableStateOf(LocalDate.now()) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            DeltaRow(deltaPct = stat.deltaPct, comparisonLabel = "tháng trước", modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    AthleticIcons.Calendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    month.month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi")) + " " + month.year,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDistanceUnit(stat.distanceMeters),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            PeriodIconBadge()
        }
        MonthlyCalendarGrid(
            month = month,
            days = days,
            selectedDate = selectedDate,
            onSelect = { selectedDate = it },
            modifier = Modifier.fillMaxWidth(),
        )
        if (trendKm.any { it > 0.0 }) {
            StravaLabel("6 tháng qua")
            MiniTrendChart(trendKm, modifier = Modifier.fillMaxWidth().height(100.dp))
        }
        ProfileStatGrid(stat)
    }
}

@Composable
private fun DeltaRow(deltaPct: Double?, comparisonLabel: String, modifier: Modifier = Modifier) {
    if (deltaPct == null) return
    val up = deltaPct >= 0
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            if (up) AthleticIcons.TrendUp else AthleticIcons.TrendDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "${if (up) "Tăng" else "Giảm"} ${abs(deltaPct).roundToInt()}% so với $comparisonLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PeriodIconBadge() {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AthleticIcons.Running, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProfileStatGrid(stat: PeriodStat) {
    val paceSecPerKm = if (stat.distanceMeters > 0) {
        stat.movingTime.inWholeSeconds / (stat.distanceMeters / 1000.0)
    } else {
        0.0
    }
    IconStatGrid(
        listOfNotNull(
            IconStatTileData(AthleticIcons.Distance, "Quãng đường", formatDistanceUnit(stat.distanceMeters)),
            IconStatTileData(AthleticIcons.Pace, "Nhịp độ TB", formatPaceUnit(paceSecPerKm)),
            IconStatTileData(AthleticIcons.Walking, "Bước chân", "%,d".format(stat.steps)),
            IconStatTileData(AthleticIcons.Duration, "Thời gian", formatHours(stat.movingTime.inWholeSeconds)),
            if (FeatureFlags.HEART_RATE_INTEGRATION) IconStatTileData(
                AthleticIcons.HeartRate,
                "Nhịp tim TB",
                stat.avgHeartRate?.toString() ?: "—",
                unit = stat.avgHeartRate?.let { "bpm" },
            ) else null,
            IconStatTileData(
                AthleticIcons.Calories,
                "Calo",
                if (stat.calories > 0) stat.calories.toString() else "—",
                unit = if (stat.calories > 0) "kcal" else null,
            ),
        ),
    )
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
            AthleticIcons.Next,
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

private fun formatHours(totalSeconds: Long): String {
    if (totalSeconds < 3600) return formatClock(totalSeconds)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (m == 0L) "$h giờ" else "${h}g ${m}p"
}
