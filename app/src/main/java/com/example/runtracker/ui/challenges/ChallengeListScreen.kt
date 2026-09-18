package com.example.runtracker.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.model.ChallengeStanding
import com.example.runtracker.ui.components.ChallengeRunCard
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.theme.Spacing
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
            ExtendedFloatingActionButton(
                onClick = onCreateChallenge,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Tạo thử thách") },
            )
        },
    ) { padding ->
        if (standings.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Chưa có thử thách",
                    message = "Đặt mục tiêu quãng đường, số buổi, độ cao hoặc thời gian trong một khoảng ngày — tiến độ tự cập nhật từ các buổi tập.",
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(standings, key = { it.challenge.id }) { standing ->
                ChallengeCard(standing, onClick = { onChallengeClick(standing.challenge.id) })
            }
        }
    }
}

private fun ChallengeGoalType.icon(): ImageVector = when (this) {
    ChallengeGoalType.TOTAL_DISTANCE -> Icons.Filled.DirectionsRun
    ChallengeGoalType.TOTAL_ACTIVITIES -> Icons.Filled.SportsScore
    ChallengeGoalType.TOTAL_ELEVATION -> Icons.Filled.Terrain
    ChallengeGoalType.TOTAL_DURATION -> Icons.Filled.Timer
}

@Composable
private fun ChallengeCard(standing: ChallengeStanding, onClick: () -> Unit) {
    val challenge = standing.challenge
    val today = remember { LocalDate.now() }
    ChallengeRunCard(
        icon = challenge.goalType.icon(),
        title = challenge.name,
        subtitle = "${challenge.goalType.formatAmount(standing.myProgress)} / ${challenge.goalType.formatAmount(challenge.goalValue)}",
        progress = standing.fraction,
        dateRangeText = "${challenge.startDate.format(DATE_FORMAT)}–${challenge.endDate.format(DATE_FORMAT)}",
        statusLabel = challenge.statusOn(today).label(),
        onClick = onClick,
    )
}
