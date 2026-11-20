package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.ui.components.RequestInboxEmpty
import com.uvg.cc3087.myapp.ui.components.RequestInboxFilters
import com.uvg.cc3087.myapp.ui.components.RequestInboxSummary
import com.uvg.cc3087.myapp.ui.components.RequestSummaryCard
import com.uvg.cc3087.myapp.ui.components.ResponseCachedNotice
import com.uvg.cc3087.myapp.ui.components.ResponseLoadPanel
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
    onForms: () -> Unit = onBack,
    onlyUnread: Boolean = false,
    onOnlyUnreadChange: (Boolean) -> Unit = {},
    hasActiveFilters: Boolean = statusFilter != "ALL" || onlyUnread,
    onClearFilters: () -> Unit = {},
    isRefreshing: Boolean = false,
    showCachedWarning: Boolean = false,
    onSettingsClick: (() -> Unit)? = null
) {
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hasContent = uiState.status == RequestListLoadStatus.CONTENT || uiState.status == RequestListLoadStatus.EMPTY
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.requests_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
                        }
                    },
                    actions = {
                        if (uiState.status != RequestListLoadStatus.UNAVAILABLE) {
                            IconButton(onClick = onRefresh,
                                enabled = !isRefreshing && uiState.status != RequestListLoadStatus.LOADING) {
                                Icon(Icons.Outlined.Refresh, stringResource(R.string.requests_refresh))
                            }
                        }
                    }
                )
                if (isRefreshing && hasContent) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("requests-refreshing"))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            FormsBottomBar(
                onMessage = { message -> scope.launch { snackbar.showSnackbar(resources.getString(message)) } },
                onResponsesClick = {},
                onFormsClick = onForms,
                onSettingsClick = onSettingsClick,
                responsesSelected = true,
                hasUnreadResponses = hasContent && hasUnreadResponses
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = 960.dp).fillMaxSize().testTag("request-list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (isDemo) item("demo-notice") {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                        Text(stringResource(R.string.requests_demo), Modifier.fillMaxWidth().padding(16.dp))
                    }
                }
                if (hasContent) {
                    if (showCachedWarning) item("cached-warning") { ResponseCachedNotice(onRefresh) }
                    item("filters") {
                        RequestInboxFilters(filters, statusFilter, onStatusFilter, hasUnknownStatus,
                            onlyUnread, onOnlyUnreadChange, hasActiveFilters, onClearFilters)
                    }
                    item("counts") { RequestInboxSummary(uiState) }
                    if (uiState.requests.isNotEmpty()) {
                        items(uiState.requests, key = { it.key }) { request -> RequestSummaryCard(request, onOpenRequest) }
                    } else {
                        item("empty") { RequestInboxEmpty(uiState.sourceEmpty, onClearFilters) }
                    }
                } else {
                    item("load-status") { ResponseLoadPanel(uiState.status, onRefresh) }
                }
            }
        }
    }
}
