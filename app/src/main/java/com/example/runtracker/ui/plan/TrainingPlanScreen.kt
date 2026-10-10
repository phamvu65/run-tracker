package com.example.runtracker.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.domain.model.PlanWeek
import com.example.runtracker.domain.model.PlannedSession
import com.example.runtracker.domain.model.TrainingPlan
import com.example.runtracker.domain.training.RaceDistance
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.SuccessCheckModal
import com.example.runtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FULL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var justSavedGoal by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            com.example.runtracker.ui.components.CompactTopHeader(
                title = "Kế hoạch tập luyện",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                PlanUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                PlanUiState.NoGoal -> GoalForm(
                    onSave = { d, date, targetTimeSec, days, longRunDay ->
                        viewModel.saveGoal(d, date, targetTimeSec, days, longRunDay)
                        editing = false
                        justSavedGoal = true
                    },
                )

                is PlanUiState.Ready -> {
                    if (editing) {
                        GoalForm(
                            initialGoal = s.plan.goal,
                            onSave = { d, date, targetTimeSec, days, longRunDay ->
                                viewModel.saveGoal(d, date, targetTimeSec, days, longRunDay)
                                editing = false
                                justSavedGoal = true
                            },
                            onCancel = { editing = false },
                        )
                    } else {
                        PlanContent(
                            plan = s.plan,
                            onEdit = { editing = true },
                            onClear = { viewModel.clearGoal() },
                        )
                    }
                }
            }
        }
    }

    if (justSavedGoal) {
        SuccessCheckModal(
            message = "Đã lưu mục tiêu!",
            onDismiss = { justSavedGoal = false },
        )
    }
}

@Composable
private fun PlanContent(plan: TrainingPlan, onEdit: () -> Unit, onClear: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "${plan.goal.raceDistance.label} · ${plan.goal.raceDate.format(FULL_DATE)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${plan.totalWeeks} tuần · khối lượng tăng dần rồi giảm tải trước giải",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(onClick = onEdit) { Text("Đổi mục tiêu") }
                    TextButton(onClick = onClear) { Text("Xoá mục tiêu") }
                }
            }
        }
        items(plan.weeks, key = { it.startDate.toString() }) { week ->
            WeekCard(week)
        }
    }
}

@Composable
private fun WeekCard(week: PlanWeek) {
    FlatCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Tuần ${week.index} · ${week.phase.label()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${trimZero(week.targetDistanceKm)} km",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            week.sessions.forEach { SessionRow(it) }
        }
    }
}

@Composable
private fun SessionRow(session: PlannedSession) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            session.date.dayOfWeek.viShort(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(session.type.label)
                    session.distanceKm?.let { append(" · ${trimZero(it)} km") }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                session.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalForm(
    initialGoal: com.example.runtracker.domain.model.TrainingGoal? = null,
    onSave: (distance: RaceDistance, date: LocalDate, targetTimeSec: Long?, daysPerWeek: Int, longRunDay: java.time.DayOfWeek) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val today = remember { LocalDate.now() }
    var distance by remember { mutableStateOf(initialGoal?.raceDistance ?: RaceDistance.TEN_K) }
    var dateText by remember { mutableStateOf((initialGoal?.raceDate ?: today.plusWeeks(12)).toString()) }
    var daysPerWeek by remember { mutableStateOf(initialGoal?.daysPerWeek ?: 4) }
    var longRunDay by remember { mutableStateOf(initialGoal?.longRunDay ?: java.time.DayOfWeek.SUNDAY) }
    var targetTimeText by remember {
        mutableStateOf(
            initialGoal?.targetTimeSeconds?.let { formatClock(it) } ?: "",
        )
    }

    val parsedDate = remember(dateText) { runCatching { LocalDate.parse(dateText) }.getOrNull() }
    val valid = parsedDate != null && parsedDate.isAfter(today)
    val parsedTargetTimeSec = remember(targetTimeText) { parseTargetTimeSeconds(targetTimeText) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Chọn cự ly mục tiêu", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RaceDistance.entries.forEach { d ->
                FilterChip(
                    selected = distance == d,
                    onClick = { distance = d },
                    label = { Text(d.label) },
                )
            }
        }

        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            label = { Text("Ngày thi đấu (yyyy-MM-dd)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(8L, 12L, 16L).forEach { weeks ->
                AssistChip(
                    onClick = { dateText = today.plusWeeks(weeks).toString() },
                    label = { Text("$weeks tuần") },
                )
            }
        }

        Text("Số buổi tập / tuần", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 4, 5).forEach { count ->
                FilterChip(
                    selected = daysPerWeek == count,
                    onClick = { daysPerWeek = count },
                    label = { Text("$count buổi/tuần") },
                )
            }
        }

        Text("Ngày chạy dài (Long Run)", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY).forEach { day ->
                FilterChip(
                    selected = longRunDay == day,
                    onClick = { longRunDay = day },
                    label = { Text(if (day == java.time.DayOfWeek.SATURDAY) "Thứ 7" else "Chủ Nhật") },
                )
            }
        }

        OutlinedTextField(
            value = targetTimeText,
            onValueChange = { targetTimeText = it },
            label = { Text("Thời gian mục tiêu (hh:mm:ss, tuỳ chọn)") },
            placeholder = { Text("Ví dụ: 01:55:00 hoặc 00:50:00") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!valid) {
            Text(
                "Ngày thi đấu phải sau hôm nay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    parsedDate?.let { date ->
                        onSave(distance, date, parsedTargetTimeSec, daysPerWeek, longRunDay)
                    }
                },
                enabled = valid,
            ) { Text("Lưu mục tiêu") }
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Huỷ") }
            }
        }

        Text(
            "Kế hoạch áp dụng Quy tắc 10% tuần và sinh lại từ fitness hiện tại mỗi lần mở.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}

private fun parseTargetTimeSeconds(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val parts = trimmed.split(":")
    return when (parts.size) {
        3 -> {
            val h = parts[0].toLongOrNull() ?: return null
            val m = parts[1].toLongOrNull() ?: return null
            val s = parts[2].toLongOrNull() ?: return null
            h * 3600 + m * 60 + s
        }
        2 -> {
            val m = parts[0].toLongOrNull() ?: return null
            val s = parts[1].toLongOrNull() ?: return null
            m * 60 + s
        }
        else -> null
    }
}

private fun trimZero(km: Double): String =
    if (km % 1.0 == 0.0) km.toInt().toString() else "%.1f".format(km)
