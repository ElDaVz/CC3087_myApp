package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.ui.resources.labelRes
import com.uvg.cc3087.myapp.ui.state.RequestListUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RequestInboxFilters(
    filters: @Composable () -> Unit,
    statusFilter: String,
    onStatusFilter: (String) -> Unit,
    hasUnknownStatus: Boolean,
    onlyUnread: Boolean,
    onOnlyUnreadChange: (Boolean) -> Unit,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.dashboard_filters), style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() })
            filters()
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf("ALL") + RequestStatus.entries.map { it.name } +
                    if (hasUnknownStatus || statusFilter == "UNKNOWN") listOf("UNKNOWN") else emptyList()
                options.forEach { option ->
                    FilterChip(
                        selected = statusFilter == option,
                        onClick = { onStatusFilter(option) },
                        modifier = Modifier.testTag("status-$option"),
                        label = {
                            Text(stringResource(when (option) {
                                "ALL" -> R.string.dashboard_all_statuses
                                "UNKNOWN" -> R.string.dashboard_unknown_status
                                else -> RequestStatus.valueOf(option).labelRes
                            }))
                        }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = onlyUnread, onClick = { onOnlyUnreadChange(!onlyUnread) },
                    modifier = Modifier.testTag("requests-only-unread"),
                    label = { Text(stringResource(R.string.requests_only_unread)) })
                if (hasActiveFilters) {
                    TextButton(onClick = onClearFilters, modifier = Modifier.testTag("requests-clear-filters")) {
                        Text(stringResource(R.string.requests_clear_filters))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RequestInboxSummary(state: RequestListUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.requests_visible_summary), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.requests_total, state.requests.size),
            style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("requests-visible-total"))
        Text(stringResource(R.string.dashboard_unread_count, state.unreadCount),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        val counts = state.statusCounts
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RequestStatus.entries.forEach { status ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.requests_status_count, stringResource(status.labelRes), counts[status] ?: 0),
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp))
                }
            }
            if (state.unknownStatusCount > 0) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.dashboard_unknown_count, state.unknownStatusCount),
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun RequestInboxEmpty(sourceEmpty: Boolean, onClearFilters: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(if (sourceEmpty) R.string.requests_empty else R.string.requests_filter_empty),
                style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (sourceEmpty) R.string.dashboard_empty_hint else R.string.requests_filter_empty_hint),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!sourceEmpty) {
                OutlinedButton(onClick = onClearFilters) { Text(stringResource(R.string.requests_clear_filters)) }
            }
        }
    }
}
