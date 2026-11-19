package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.ui.components.DashboardCachedNotice
import com.uvg.cc3087.myapp.ui.components.DashboardEmptyPanel
import com.uvg.cc3087.myapp.ui.components.DashboardFilterPanel
import com.uvg.cc3087.myapp.ui.components.DashboardLoadPanel
import com.uvg.cc3087.myapp.ui.components.DashboardOverview
import com.uvg.cc3087.myapp.ui.components.RequestSummaryCard
import com.uvg.cc3087.myapp.ui.components.ResponseActivityChart
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseDashboardScreen(
    state: ResponseDashboardUiState,
    listState: LazyListState,
    onForm: (String?) -> Unit,
    onPeriod: (ResponsePeriod) -> Unit,
    onInbox: (RequestStatus?) -> Unit,
    onUnknown: () -> Unit,
    onOpen: (RequestSummary) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    syncMessage: String? = null,
    isDemo: Boolean = false
) {
    val data = state.dashboard
    val showData = state.hasData && state.status != RequestListLoadStatus.FORBIDDEN
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.dashboard_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !state.isRefreshing && state.status != RequestListLoadStatus.LOADING
                        ) {
                            Icon(Icons.Outlined.Refresh, stringResource(R.string.requests_refresh))
                        }
                    }
                )
                if (state.isRefreshing && showData) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("dashboard-refreshing"))
                }
            }
        },
        bottomBar = {
            FormsBottomBar(
                onMessage = { message ->
                    scope.launch { snackbar.showSnackbar(resources.getString(message)) }
                },
                onResponsesClick = {},
                onFormsClick = onBack,
                responsesSelected = true,
                hasUnreadResponses = showData && data.hasUnread
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = 960.dp).fillMaxSize().testTag("response-dashboard"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (isDemo) item("demo") { Text(stringResource(R.string.requests_demo)) }
                syncMessage?.let { message ->
                    item("sync") {
                        Text(message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (showData) {
                    if (state.status != RequestListLoadStatus.CONTENT) {
                        item("cached-warning") { DashboardCachedNotice(onRefresh) }
                    }
                    item("filters") { DashboardFilterPanel(state.filter, data.forms, onForm, onPeriod) }
                    item("overview") { DashboardOverview(data, onInbox, onUnknown) }
                    if (data.requests.isNotEmpty()) {
                        item("activity") { ResponseActivityChart(data.buckets, state.filter) }
                        if (data.undatedCount > 0) {
                            item("undated") {
                                Text(stringResource(R.string.dashboard_undated, data.undatedCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item("recent-heading") {
                            Text(stringResource(R.string.dashboard_recent), style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics { heading() })
                        }
                        items(data.requests.take(5), key = { it.key }) { RequestSummaryCard(it, onOpen) }
                    } else {
                        item("empty") {
                            DashboardEmptyPanel(data.sourceEmpty) {
                                onForm(null)
                                onPeriod(ResponsePeriod.ALL)
                            }
                        }
                    }
                    item("inbox") {
                        FilledTonalButton(
                            onClick = { onInbox(null) },
                            modifier = Modifier.fillMaxWidth().testTag("open-inbox")
                        ) { Text(stringResource(R.string.dashboard_view_all)) }
                    }
                } else {
                    item("load-state") { DashboardLoadPanel(state.status, onRefresh) }
                }
            }
        }
    }
}

