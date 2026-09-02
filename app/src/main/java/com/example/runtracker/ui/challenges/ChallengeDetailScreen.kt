package com.example.runtracker.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeParticipant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChallengeDetailViewModel = hiltViewModel(),
) {
    val challenge by viewModel.challenge.collectAsState()
    val participants by viewModel.participants.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.deleted) {
        if (viewModel.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(challenge?.name ?: "Thử thách") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Xoá thử thách")
                    }
                },
            )
        },
    ) { padding ->
        val c = challenge
        if (c == null) {
            return@Scaffold
        }

        val today = remember { LocalDate.now() }
        val myProgress = participants.firstOrNull { it.userId == LOCAL_USER_ID }?.currentProgress ?: 0.0
        val fraction = if (c.goalValue > 0) (myProgress / c.goalValue).coerceIn(0.0, 1.0).toFloat() else 0f

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${c.goalType.label()} · ${c.goalType.formatAmount(c.goalValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${c.startDate.format(DATE_FORMAT)} – ${c.endDate.format(DATE_FORMAT)} · " +
                        c.statusOn(today).label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${c.goalType.formatAmount(myProgress)} / ${c.goalType.formatAmount(c.goalValue)}" +
                        "  (${(fraction * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text("Bảng xếp hạng (${participants.size})", style = MaterialTheme.typography.titleSmall)
            if (participants.isEmpty()) {
                Text(
                    "Chưa có người tham gia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                participants.forEachIndexed { index, participant ->
                    ParticipantRow(index + 1, participant, c)
                    HorizontalDivider()
                }
            }

            Text(
                "Thử thách nhóm nhiều người sẽ khả dụng khi có backend đồng bộ. Hiện tiến độ tính " +
                    "từ các buổi tập của bạn trong khoảng ngày trên.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá thử thách?") },
            text = { Text("Thử thách và tiến độ sẽ bị xoá khỏi thiết bị.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Xoá") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun ParticipantRow(rank: Int, participant: ChallengeParticipant, challenge: Challenge) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("$rank", style = MaterialTheme.typography.bodyMedium)
        Text(
            if (participant.userId == LOCAL_USER_ID) "Bạn" else participant.userId,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            challenge.goalType.formatAmount(participant.currentProgress),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
