package com.txnrelay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.txnrelay.config.PaymentApp
import com.txnrelay.data.model.EventStatus
import com.txnrelay.ui.EventUi
import com.txnrelay.ui.theme.BkashBadge
import com.txnrelay.ui.theme.LocalStatusPalette
import com.txnrelay.ui.theme.NagadBadge
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventRow(event: EventUi, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppBadge(event.app)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title.ifBlank { event.app?.displayName ?: "Notification" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(event.postedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (event.body.isNotBlank()) {
                    Text(
                        text = event.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    StatusChip(event.status)
                    val detail = when {
                        event.status == EventStatus.PENDING && event.retryCount > 0 ->
                            "Retried ${event.retryCount}× · ${event.lastError.orEmpty()}"
                        event.status == EventStatus.FAILED_PERMANENT -> event.lastError.orEmpty()
                        else -> ""
                    }
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: EventStatus, modifier: Modifier = Modifier) {
    val p = LocalStatusPalette.current
    val (label, fg, bg) = when (status) {
        EventStatus.SENT -> Triple("Sent", p.sent, p.sentBg)
        EventStatus.PENDING -> Triple("Pending", p.pending, p.pendingBg)
        EventStatus.FAILED_PERMANENT -> Triple("Failed", p.failed, p.failedBg)
    }
    Text(
        text = label,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun AppBadge(app: PaymentApp?) {
    val color: Color = when (app) {
        PaymentApp.BKASH -> BkashBadge
        PaymentApp.NAGAD -> NagadBadge
        null -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = (app?.displayName ?: "?").take(1),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun EmptyEvents(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "No events yet",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "bKash and Nagad notifications will appear here. Tap \"Test event\" to preview the flow.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault())

private fun formatTimestamp(epochMs: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
