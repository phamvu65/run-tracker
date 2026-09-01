package com.example.runtracker.ui.zones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.domain.training.HeartRateZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ZoneSettingsViewModel = hiltViewModel(),
) {
    val zones by viewModel.zones.collectAsState()
    val estimatedMaxHr by viewModel.estimatedMaxHr.collectAsState()

    LaunchedEffect(viewModel.saved) {
        if (viewModel.saved) {
            viewModel.consumeSaved()
            onBack()
        }
    }

    val loaded = zones ?: run {
        Scaffold(modifier = modifier, topBar = { ZoneTopBar(onBack) }) { p ->
            Text("Đang tải…", Modifier.padding(p).padding(16.dp))
        }
        return
    }

    // Form state: min/max của từng vùng dưới dạng chuỗi.
    val mins = remember(loaded) { mutableStateListOf(*loaded.zones.map { it.minBpm.toString() }.toTypedArray()) }
    val maxs = remember(loaded) { mutableStateListOf(*loaded.zones.map { it.maxBpm.toString() }.toTypedArray()) }
    var thresholdPace by remember(loaded) {
        mutableStateOf(loaded.thresholdPaceSecPerKm?.let { formatPaceInput(it) } ?: "")
    }

    Scaffold(modifier = modifier, topBar = { ZoneTopBar(onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                estimatedMaxHr?.let { "Nhịp tim tối đa: $it bpm (từ hồ sơ)" }
                    ?: "Chưa có nhịp tim tối đa — cập nhật ở Hồ sơ để có mặc định tốt hơn.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            loaded.zones.forEachIndexed { i, _ ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Z${i + 1}", Modifier.width(32.dp), style = MaterialTheme.typography.titleSmall)
                    BpmField(mins[i], { mins[i] = it }, "Min", Modifier.weight(1f))
                    BpmField(maxs[i], { maxs[i] = it }, "Max", Modifier.weight(1f))
                }
            }

            OutlinedTextField(
                value = thresholdPace,
                onValueChange = { thresholdPace = it },
                label = { Text("Pace ngưỡng (m:ss/km, tuỳ chọn)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.defaultZones().forEachIndexed { i, z ->
                            mins[i] = z.minBpm.toString()
                            maxs[i] = z.maxBpm.toString()
                        }
                    },
                ) { Text("Đặt lại theo % maxHR") }

                Button(
                    onClick = {
                        val zoneList = (0 until 5).map { i ->
                            HeartRateZone(
                                index = i + 1,
                                minBpm = mins[i].toIntOrNull() ?: 0,
                                maxBpm = maxs[i].toIntOrNull() ?: 0,
                            )
                        }
                        viewModel.save(zoneList, parsePaceInput(thresholdPace))
                    },
                ) { Text("Lưu") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Vùng nhịp tim") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        },
    )
}

@Composable
private fun BpmField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) onValueChange(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

private fun formatPaceInput(secPerKm: Double): String {
    val total = secPerKm.toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

private fun parsePaceInput(text: String): Double? {
    val parts = text.trim().split(":")
    return when (parts.size) {
        2 -> {
            val m = parts[0].toIntOrNull() ?: return null
            val s = parts[1].toIntOrNull() ?: return null
            (m * 60 + s).toDouble()
        }
        1 -> parts[0].toDoubleOrNull()
        else -> null
    }
}
