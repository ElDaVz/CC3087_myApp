package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.ui.resources.labelRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RequestSummaryCard(request: RequestSummary, onOpen: (RequestSummary) -> Unit) {
    OutlinedCard(
        onClick = { onOpen(request) },
        modifier = Modifier.fillMaxWidth().testTag("request-${request.key}"),
        border = BorderStroke(1.dp, if (request.isRead) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!request.isRead) {
                        Text(stringResource(R.string.dashboard_unread), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text(request.formTitle.ifBlank { stringResource(R.string.untitled_form) },
                        style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RequestStatusBadge(request.requestStatus, request.statusLabel)
            Text(stringResource(R.string.request_detail_submitted, requestDate(request.submittedAt)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.request_detail_id, request.responseId),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun RequestStatusBadge(status: RequestStatus?, legacyLabel: String? = null) {
    val label = status?.let { stringResource(it.labelRes) }
        ?: legacyLabel?.takeIf { it.isNotBlank() } ?: stringResource(R.string.request_detail_no_status)
    val background = when (status) {
        RequestStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
        RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
        RequestStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
        null -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val foreground = when (status) {
        RequestStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
        RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
        RequestStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
        null -> MaterialTheme.colorScheme.onSurface
    }
    Surface(color = background, contentColor = foreground, shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(when (status) {
                RequestStatus.PENDING -> Icons.Outlined.Schedule
                RequestStatus.IN_PROGRESS -> Icons.Outlined.PendingActions
                RequestStatus.COMPLETED -> Icons.Outlined.CheckCircleOutline
                null -> Icons.AutoMirrored.Outlined.HelpOutline
            }, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.request_detail_status, label), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun requestDate(instant: Instant?): String {
    if (instant == null) return stringResource(R.string.request_detail_no_date)
    val locale = LocalResources.current.configuration.locales[0]
    val zone = ZoneId.systemDefault()
    val formatter = remember(locale, zone) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).withZone(zone)
    }
    return formatter.format(instant)
}
