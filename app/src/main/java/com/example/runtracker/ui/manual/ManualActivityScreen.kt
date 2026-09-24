package com.example.runtracker.ui.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Nhập buổi tập không qua GPS (chạy treadmill, quên bật app lúc chạy ngoài trời...). Không có
 * route points nên màn chi tiết sau khi lưu sẽ tự rơi vào nhánh "Không có dữ liệu GPS" đã có sẵn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualActivityScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManualActivityViewModel = hiltViewModel(),
) {
    val now = remember { LocalDateTime.now() }
    var type by remember { mutableStateOf(ActivityType.RUNNING) }
    var date by remember { mutableStateOf(now.toLocalDate().format(DATE_FMT)) }
    var time by remember { mutableStateOf(now.toLocalTime().format(TIME_FMT)) }
    var distanceKm by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var rpeEnabled by remember { mutableStateOf(false) }
    var rpe by remember { mutableIntStateOf(5) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Nhập buổi tập") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FlatCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SectionHeader("Buổi tập", subtitle = "Không cần GPS — dùng cho treadmill hoặc quên bật app")

                    Text("Loại hoạt động", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        listOf(
                            ActivityType.RUNNING to "Chạy bộ",
                            ActivityType.WALKING to "Đi bộ",
                            ActivityType.CYCLING to "Đạp xe",
                            ActivityType.OTHER to "Khác",
                        ).forEach { (t, label) ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(label) })
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Ngày (yyyy-MM-dd)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Giờ (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    OutlinedTextField(
                        value = distanceKm,
                        onValueChange = { new -> if (new.all { it.isDigit() || it == '.' || it == ',' }) distanceKm = new },
                        label = { Text("Quãng đường (km)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { new -> if (new.all { it.isDigit() || it == '.' || it == ',' }) durationMinutes = new },
                        label = { Text("Thời gian (phút)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nhập cảm giác gắng sức (RPE)", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(Spacing.sm))
                        Switch(checked = rpeEnabled, onCheckedChange = { rpeEnabled = it })
                    }
                    if (rpeEnabled) {
                        Text("RPE: $rpe (1 rất nhẹ … 10 kiệt sức)", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = rpe.toFloat(),
                            onValueChange = { rpe = it.roundToInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                        )
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            val distance = distanceKm.trim().replace(',', '.').toDoubleOrNull()
                            val duration = durationMinutes.trim().replace(',', '.').toDoubleOrNull()
                            val parsedDate = runCatching { LocalDate.parse(date.trim(), DATE_FMT) }.getOrNull()
                            val parsedTime = runCatching { LocalTime.parse(time.trim(), TIME_FMT) }.getOrNull()
                            if (distance == null || distance <= 0.0) {
                                error = "Nhập quãng đường hợp lệ"
                            } else if (duration == null || duration <= 0.0) {
                                error = "Nhập thời gian hợp lệ"
                            } else if (parsedDate == null || parsedTime == null) {
                                error = "Ngày/giờ không đúng định dạng"
                            } else {
                                error = null
                                val startTime = LocalDateTime.of(parsedDate, parsedTime)
                                    .atZone(ZoneId.systemDefault()).toInstant()
                                viewModel.save(
                                    type = type,
                                    startTime = startTime,
                                    distanceKm = distance,
                                    durationMinutes = duration,
                                    perceivedExertion = if (rpeEnabled) rpe else null,
                                ) { id ->
                                    if (id != null) onCreated(id) else error = "Không lưu được buổi tập"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Lưu") }
                }
            }
        }
    }
}
