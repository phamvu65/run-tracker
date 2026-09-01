package com.example.runtracker.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.domain.model.Sex
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenZones: () -> Unit = {},
    onOpenRoutes: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) {
        if (saved) {
            viewModel.consumeSaved()
            onBack()
        }
    }

    var name by remember(user) { mutableStateOf(user?.displayName.orEmpty()) }
    var sex by remember(user) { mutableStateOf(user?.sex) }
    var birthYear by remember(user) { mutableStateOf(user?.birthYear?.toString().orEmpty()) }
    var weight by remember(user) { mutableStateOf(user?.weightKg?.toString().orEmpty()) }
    var restingHr by remember(user) { mutableStateOf(user?.restingHeartRate?.toString().orEmpty()) }
    var maxHr by remember(user) { mutableStateOf(user?.maxHeartRate?.toString().orEmpty()) }

    val currentYear = remember { Year.now().value }
    val estimatedMaxHr = birthYear.trim().toIntOrNull()?.let { 220 - (currentYear - it) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên hiển thị") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Giới tính (cho công thức TRIMP)", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sex == Sex.MALE,
                    onClick = { sex = Sex.MALE.takeIf { sex != Sex.MALE } },
                    label = { Text("Nam") },
                )
                FilterChip(
                    selected = sex == Sex.FEMALE,
                    onClick = { sex = Sex.FEMALE.takeIf { sex != Sex.FEMALE } },
                    label = { Text("Nữ") },
                )
            }

            NumberField(birthYear, { birthYear = it }, "Năm sinh")
            NumberField(weight, { weight = it }, "Cân nặng (kg)", decimal = true)
            NumberField(restingHr, { restingHr = it }, "Nhịp tim nghỉ (bpm)")
            NumberField(
                maxHr,
                { maxHr = it },
                "Nhịp tim tối đa (bpm)",
                supporting = when {
                    maxHr.isNotBlank() -> "Dùng giá trị đo thực tế này"
                    estimatedMaxHr != null -> "Bỏ trống → ước tính $estimatedMaxHr (220 - tuổi)"
                    else -> "Bỏ trống → cần năm sinh để ước tính"
                },
            )

            Button(
                onClick = {
                    viewModel.save(
                        displayName = name,
                        sex = sex,
                        birthYear = birthYear.trim().toIntOrNull(),
                        weightKg = weight.trim().replace(',', '.').toDoubleOrNull(),
                        restingHeartRate = restingHr.trim().toIntOrNull(),
                        maxHeartRate = maxHr.trim().toIntOrNull(),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Lưu") }

            OutlinedButton(onClick = onOpenZones, modifier = Modifier.fillMaxWidth()) {
                Text("Vùng nhịp tim")
            }
            OutlinedButton(onClick = onOpenRoutes, modifier = Modifier.fillMaxWidth()) {
                Text("Routes đã lưu")
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    decimal: Boolean = false,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            val allowed = if (decimal) new.all { it.isDigit() || it == '.' || it == ',' } else new.all { it.isDigit() }
            if (new.isEmpty() || allowed) onValueChange(new)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        supportingText = supporting?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
