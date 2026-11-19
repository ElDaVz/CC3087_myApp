package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.domain.requests.ResponseFilter
import com.uvg.cc3087.myapp.domain.requests.ResponseForm
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus

@Composable
internal fun DashboardFilterPanel(
    filter: ResponseFilter,
    forms: List<ResponseForm>,
    onForm: (String?) -> Unit,
    onPeriod: (ResponsePeriod) -> Unit
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.dashboard_filters), style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() })
            ResponseFilters(filter, forms, onForm, onPeriod)
            if (filter != ResponseFilter()) {
                TextButton(onClick = { onForm(null); onPeriod(ResponsePeriod.MONTH) }) {
                    Text(stringResource(R.string.dashboard_reset_filters))
                }
            }
        }
    }
}

@Composable
internal fun DashboardCachedNotice(onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null)
            Text(stringResource(R.string.dashboard_cached_notice), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
internal fun DashboardEmptyPanel(sourceEmpty: Boolean, onShowHistory: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(if (sourceEmpty) R.string.requests_empty else R.string.dashboard_period_empty),
                style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (sourceEmpty) R.string.dashboard_empty_hint else R.string.dashboard_filtered_hint),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!sourceEmpty) {
                OutlinedButton(onClick = onShowHistory) { Text(stringResource(R.string.dashboard_show_history)) }
            }
        }
    }
}

@Composable
internal fun DashboardLoadPanel(status: RequestListLoadStatus, onRetry: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (status != RequestListLoadStatus.LOADING) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            ResponseLoadMessage(status, onRetry)
        }
    }
}
