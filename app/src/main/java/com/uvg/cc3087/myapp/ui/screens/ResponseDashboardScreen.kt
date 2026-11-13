package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.ui.components.*
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.domain.requests.*
import com.uvg.cc3087.myapp.ui.resources.labelRes
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardUiState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResponseDashboardScreen(state: ResponseDashboardUiState, listState: LazyListState,
    onForm: (String?) -> Unit, onPeriod: (ResponsePeriod) -> Unit,
    onInbox: (RequestStatus?) -> Unit, onUnknown: () -> Unit, onOpen: (RequestSummary) -> Unit,
    onBack: () -> Unit, onRefresh: () -> Unit, syncMessage: String? = null, isDemo: Boolean = false) {
    val data = state.dashboard
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back)) } },
        actions = { IconButton(onClick = onRefresh, enabled = state.status != RequestListLoadStatus.LOADING) {
            Icon(Icons.Outlined.Refresh, stringResource(R.string.requests_refresh))
        } }) }, bottomBar = {
        FormsBottomBar({ message -> scope.launch { snackbar.showSnackbar(resources.getString(message)) } }, {}, onFormsClick = onBack, responsesSelected = true, hasUnreadResponses = data.hasUnread)
    }) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding).testTag("response-dashboard"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (isDemo) item { Text(stringResource(R.string.requests_demo)) }
            syncMessage?.let { item { Text(it, style = MaterialTheme.typography.bodySmall) } }
            if (state.hasData) {
                item { ResponseFilters(state.filter, data.forms, onForm, onPeriod) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(stringResource(R.string.dashboard_total), data.requests.size, Modifier.weight(1f).widthIn(min = 140.dp).testTag("metric-total")) { onInbox(null) }
                        RequestStatus.entries.forEach { status ->
                            MetricCard(stringResource(status.labelRes), data.counts[status] ?: 0,
                                Modifier.weight(1f).widthIn(min = 140.dp).testTag("metric-${status.name}")) { onInbox(status) }
                        }
                    }
                    if (data.unknownStatusCount > 0) TextButton(onClick = onUnknown) {
                        Text(stringResource(R.string.dashboard_unknown_count, data.unknownStatusCount))
                    }
                }
                if (data.requests.isNotEmpty()) {
                    item { ResponseChart(data.buckets, state.filter.period) }
                    if (data.undatedCount > 0) item { Text(stringResource(R.string.dashboard_undated, data.undatedCount)) }
                    item { Text(stringResource(R.string.dashboard_recent), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
                    items(data.requests.take(5), key = { it.key }) { RequestSummaryCard(it, onOpen) }
                } else item { Text(stringResource(if (data.sourceEmpty) R.string.requests_empty else R.string.dashboard_period_empty)) }
                item { FilledTonalButton(onClick = { onInbox(null) }, modifier = Modifier.fillMaxWidth().testTag("open-inbox")) {
                    Text(stringResource(R.string.dashboard_view_all))
                } }
            }
            if (state.status != RequestListLoadStatus.CONTENT) item {
                ResponseLoadMessage(state.status, onRefresh)
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, count: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineLarge)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ResponseChart(buckets: List<ResponseBucket>, period: ResponsePeriod) {
    val locale = LocalResources.current.configuration.locales[0]
    val formatter = remember(locale, period) { DateTimeFormatter.ofPattern(if (period == ResponsePeriod.ALL) "MMM yyyy" else "d MMM", locale) }
    val height = (buckets.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dashboard_evolution), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(if (period == ResponsePeriod.ALL) R.string.dashboard_monthly else R.string.dashboard_daily), style = MaterialTheme.typography.bodySmall)
        val chartState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(period, buckets.size) { if (buckets.isNotEmpty()) chartState.scrollToItem(buckets.lastIndex) }
        LazyRow(state = chartState, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().testTag("response-chart")) {
            items(buckets, key = { it.date.toString() }) { bucket ->
                val label = formatter.format(bucket.date)
                val description = stringResource(R.string.dashboard_chart_value, label, bucket.count)
                Column(Modifier.width(68.dp).semantics(mergeDescendants = true) { contentDescription = description }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(bucket.count.toString(), style = MaterialTheme.typography.labelMedium)
                    Box(Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.width(24.dp).height((100f * bucket.count / height).dp.coerceAtLeast(2.dp))
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

