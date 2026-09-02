package com.example.runtracker.ui.challenges

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.domain.model.ChallengeStanding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeListScreen(
    onBack: (() -> Unit)? = null,
    onChallengeClick: (String) -> Unit,
    onCreateChallenge: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChallengeListViewModel = hiltViewModel(),
) {
    val standings by viewModel.standings.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Thử thách") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateChallenge) {
                Icon(Icons.Filled.Add, contentDescription = "Tạo thử thách")
            }
        },
    ) { padding ->
        if (standings.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Chưa có thử thách. Bấm + để tạo mục tiêu quãng đường, số buổi hoặc thời gian.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(standings, key = { it.challenge.id }) { standing ->
                ChallengeRow(standing, onClick = { onChallengeClick(standing.challenge.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ChallengeRow(standing: ChallengeStanding, onClick: () -> Unit) {
    val challenge = standing.challenge
    val today = remember { LocalDate.now() }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(challenge.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                challenge.statusOn(today).label(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { standing.fraction },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "${challenge.goalType.formatAmount(standing.myProgress)} / " +
                "${challenge.goalType.formatAmount(challenge.goalValue)} · " +
                "${challenge.startDate.format(DATE_FORMAT)}–${challenge.endDate.format(DATE_FORMAT)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
