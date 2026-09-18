package com.example.runtracker.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.ui.components.SuccessCheckModal
import com.example.runtracker.ui.theme.Spacing
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateChallengeViewModel = hiltViewModel(),
) {
    val today = remember { LocalDate.now() }
    var name by remember { mutableStateOf("") }
    var goalType by remember { mutableStateOf(ChallengeGoalType.TOTAL_DISTANCE) }
    var goalValue by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(today.toString()) }
    var end by remember { mutableStateOf(today.plusDays(30).toString()) }

    val startDate = remember(start) { runCatching { LocalDate.parse(start) }.getOrNull() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tạo thử thách") },
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
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên thử thách") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Loại mục tiêu", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ChallengeGoalType.entries.forEach { type ->
                    FilterChip(
                        selected = goalType == type,
                        onClick = { goalType = type },
                        label = { Text(type.label()) },
                    )
                }
            }

            OutlinedTextField(
                value = goalValue,
                onValueChange = { new ->
                    if (new.isEmpty() || new.all { it.isDigit() || it == '.' || it == ',' }) goalValue = new
                },
                label = { Text("Giá trị mục tiêu (${goalType.inputUnit()})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = start,
                onValueChange = { start = it },
                label = { Text("Bắt đầu (yyyy-MM-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = end,
                onValueChange = { end = it },
                label = { Text("Kết thúc (yyyy-MM-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf(7L, 30L, 90L).forEach { days ->
                    AssistChip(
                        onClick = {
                            val base = startDate ?: today
                            end = base.plusDays(days).toString()
                        },
                        label = { Text("$days ngày") },
                    )
                }
            }

            viewModel.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    viewModel.create(
                        name = name,
                        goalType = goalType,
                        inputValue = goalValue.trim().replace(',', '.').toDoubleOrNull(),
                        startDate = startDate,
                        endDate = runCatching { LocalDate.parse(end) }.getOrNull(),
                    )
                },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (viewModel.saving) "Đang tạo…" else "Tạo thử thách") }
        }
    }

    val savedId = viewModel.savedId
    if (savedId != null) {
        SuccessCheckModal(
            message = "Đã tạo thử thách!",
            onDismiss = {
                viewModel.consumeSaved()
                onCreated(savedId)
            },
        )
    }
}
