package com.example.runtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.runtracker.ui.theme.AccentWarning
import com.example.runtracker.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Modal xác nhận thành công kiểu GoRun "Modal Card": vòng tròn dấu tick giữa các chấm tô điểm,
 * trên thẻ nền tối bo góc. Tự đóng sau [autoDismissMillis] hoặc khi bấm ra ngoài — dùng sau các
 * hành động lưu (hồ sơ, thử thách, route, segment, mục tiêu tập luyện...).
 */
@Composable
fun SuccessCheckModal(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMillis: Long = 1400,
) {
    LaunchedEffect(Unit) {
        delay(autoDismissMillis)
        onDismiss()
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.width(260.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    listOf(
                        Triple(MaterialTheme.colorScheme.tertiary, (-38).dp, (-30).dp),
                        Triple(MaterialTheme.colorScheme.error, 36.dp, (-22).dp),
                        Triple(AccentWarning, (-34).dp, 30.dp),
                        Triple(MaterialTheme.colorScheme.secondary, 32.dp, 28.dp),
                    ).forEach { (color, dx, dy) ->
                        Box(
                            Modifier
                                .offset(x = dx, y = dy)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
