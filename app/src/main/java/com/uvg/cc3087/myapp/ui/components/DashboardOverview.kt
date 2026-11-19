package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.domain.requests.ResponseDashboard
import com.uvg.cc3087.myapp.ui.resources.labelRes

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardOverview(
    data: ResponseDashboard,
    onInbox: (RequestStatus?) -> Unit,
    onUnknown: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.dashboard_overview), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.dashboard_filter_hint), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.dashboard_unread_count, data.unreadCount),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("dashboard-unread-count"))
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // con texto grande damos más ancho a cada tarjeta, sin cortar sus etiquetas
            val minimumWidth = 148.dp * fontScale
            val columns = when {
                maxWidth >= minimumWidth * 4 + 36.dp -> 4
                maxWidth >= minimumWidth * 2 + 12.dp -> 2
                else -> 1
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = columns,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardMetric(
                    label = stringResource(R.string.dashboard_total),
                    count = data.requests.size,
                    icon = Icons.Outlined.Inbox,
                    highlighted = true,
                    modifier = Modifier.weight(1f).fillMaxRowHeight().testTag("metric-total"),
                    onClick = { onInbox(null) }
                )
                RequestStatus.entries.forEach { status ->
                    DashboardMetric(
                        label = stringResource(status.labelRes),
                        count = data.counts[status] ?: 0,
                        icon = when (status) {
                            RequestStatus.PENDING -> Icons.Outlined.Schedule
                            RequestStatus.IN_PROGRESS -> Icons.Outlined.PendingActions
                            RequestStatus.COMPLETED -> Icons.Outlined.CheckCircleOutline
                        },
                        highlighted = false,
                        modifier = Modifier.weight(1f).fillMaxRowHeight().testTag("metric-${status.name}"),
                        onClick = { onInbox(status) }
                    )
                }
            }
        }
        if (data.unknownStatusCount > 0) {
            TextButton(onClick = onUnknown) {
                Text(stringResource(R.string.dashboard_unknown_count, data.unknownStatusCount))
            }
        }
        if (data.requests.isNotEmpty()) {
            val completed = data.counts[RequestStatus.COMPLETED] ?: 0
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dashboard_progress), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.dashboard_completed_count, completed, data.requests.size),
                        style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / data.requests.size },
                        modifier = Modifier.fillMaxWidth().testTag("dashboard-completion-progress")
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    count: Int,
    icon: ImageVector,
    highlighted: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineLarge)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
