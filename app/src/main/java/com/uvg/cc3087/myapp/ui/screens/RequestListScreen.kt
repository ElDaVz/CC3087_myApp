package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.ui.components.RequestSummaryCard
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.ui.resources.labelRes
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestListUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    uiState: RequestListUiState,
    onOpenRequest: (RequestSummary) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    listState: LazyListState,
    isDemo: Boolean = false,
    hasUnreadResponses: Boolean = false,
    filters: @Composable () -> Unit = {},
    statusFilter: String = "ALL",
    onStatusFilter: (String) -> Unit = {},
    hasUnknownStatus: Boolean = false,
    onForms: () -> Unit = onBack
) {
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.requests_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
                    }
                },
                actions = {
                    if (uiState.status != RequestListLoadStatus.UNAVAILABLE) {
                        IconButton(onClick = onRefresh, enabled = uiState.status != RequestListLoadStatus.LOADING) {
                            Icon(Icons.Outlined.Refresh, stringResource(R.string.requests_refresh))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            FormsBottomBar(
                onMessage = { message -> scope.launch { snackbar.showSnackbar(resources.getString(message)) } },
                onResponsesClick = {},
                onFormsClick = onForms,
                responsesSelected = true,
                hasUnreadResponses = hasUnreadResponses
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("request-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "filters") {
                filters()
                androidx.compose.foundation.layout.Row(
                    Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf("ALL") + RequestStatus.entries.map { it.name } + if (hasUnknownStatus) listOf("UNKNOWN") else emptyList()
                    options.forEach { option ->
                        FilterChip(selected = statusFilter == option, onClick = { onStatusFilter(option) },
                            modifier = Modifier.testTag("status-$option"), label = {
                                Text(stringResource(when (option) {
                                    "ALL" -> R.string.dashboard_all_statuses
                                    "UNKNOWN" -> R.string.dashboard_unknown_status
                                    else -> RequestStatus.valueOf(option).labelRes
                                }))
                            })
                    }
                }
            }
            if (isDemo) item(key = "demo-notice") {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.requests_demo), Modifier.fillMaxWidth().padding(16.dp))
                }
            }
            if (uiState.status == RequestListLoadStatus.CONTENT || uiState.status == RequestListLoadStatus.EMPTY) item(key = "counts") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.requests_total, uiState.requests.size))
                    RequestStatus.entries.forEach { status ->
                        Text(stringResource(R.string.requests_status_count, stringResource(status.labelRes),
                            uiState.requests.count { it.requestStatus == status }))
                    }
                }
            }
            if (uiState.status == RequestListLoadStatus.CONTENT) {
                items(uiState.requests, key = { it.key }) { request ->
                    RequestSummaryCard(request, onOpenRequest)
                }
            } else item(key = "load-status") {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.status == RequestListLoadStatus.LOADING) CircularProgressIndicator()
                    Text(stringResource(when (uiState.status) {
                        RequestListLoadStatus.LOADING -> R.string.requests_loading
                        RequestListLoadStatus.EMPTY -> R.string.requests_empty
                        RequestListLoadStatus.FORBIDDEN -> R.string.requests_forbidden
                        RequestListLoadStatus.UNAVAILABLE -> R.string.requests_unavailable
                        else -> R.string.requests_error
                    }))
                    if (uiState.status == RequestListLoadStatus.ERROR || uiState.status == RequestListLoadStatus.FORBIDDEN) {
                        OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
        }
    }
}
