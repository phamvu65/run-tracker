package com.example.runtracker.ui.complete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.PersonalRecord
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.components.StatStrip
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing

/**
 * Màn hiện ra ngay sau khi kết thúc một buổi tập: có kỷ lục thì ăn mừng, không thì một câu
 * khích lệ ngẫu nhiên — hoàn thành buổi nào cũng đáng được ghi nhận, không chỉ khi có PR.
 */
@Composable
fun RunCompleteScreen(
    onClose: () -> Unit,
    onViewDetail: (activityId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunCompleteViewModel = hiltViewModel(),
) {
    val activity = viewModel.activity
    val records = viewModel.records

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onClose) { Text("Đóng") }
            }

            Spacer(Modifier.height(Spacing.lg))

            if (!viewModel.loading) {
                if (records.isNotEmpty()) {
                    AchievementHeader(records)
                } else {
                    EncouragementHeader(viewModel.quote)
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            activity?.let { a ->
                FlatCard {
                    StatStrip(
                        listOf(
                            StatCell("Quãng đường", formatDistanceKm(a.distanceMeters)),
                            StatCell("Thời gian di chuyển", formatClock(a.movingTime.inWholeSeconds)),
                            StatCell("Nhịp độ", formatPace(a.avgPaceSecPerKm)),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Button(
                onClick = { onViewDetail(viewModel.activityId) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Xem chi tiết") }
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
        }
    }
}

@Composable
private fun AchievementHeader(records: List<PersonalRecord>) {
    Text("🏆", style = MaterialTheme.typography.displayLarge)
    Spacer(Modifier.height(Spacing.sm))
    Text(
        if (records.size > 1) "Kỷ lục mới!" else "Kỷ lục mới: ${records.first().label}!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.lg))
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        records.forEach { record ->
            FlatCard {
                StravaLabel(record.label)
                Spacer(Modifier.height(2.dp))
                Text(record.valueText, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun EncouragementHeader(quote: String) {
    Text("🏁", style = MaterialTheme.typography.displayLarge)
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Hoàn thành!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(Spacing.md))
    Text(
        quote,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.md),
    )
}
