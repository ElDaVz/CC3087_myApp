package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.domain.requests.ResponseFilter
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.ui.components.ResponseFilters
import com.uvg.cc3087.myapp.ui.screens.RequestListScreen
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestListUiState
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardUiState

@Composable
internal fun ResponsesRoute(
    state: ResponseDashboardUiState, status: InboxFilter, list: LazyListState,
    onForm: (String?) -> Unit, onPeriod: (ResponsePeriod) -> Unit, onStatus: (InboxFilter) -> Unit,
    onOpen: (RequestSummary) -> Unit, onBack: () -> Unit, onForms: () -> Unit, onRefresh: () -> Unit,
    syncMessage: String?, isDemo: Boolean
) {
    // solo guardamos este booleano de presentación, no textos ni respuestas del cliente
    var onlyUnread by rememberSaveable { mutableStateOf(false) }
    val data = state.dashboard
    val canShowData = state.hasData && state.status != RequestListLoadStatus.FORBIDDEN
    val rows = if (canShowData) data.requests.filter {
        status.matches(it.requestStatus) && (!onlyUnread || !it.isRead)
    } else emptyList()
    val loadStatus = if (canShowData) {
        if (rows.isEmpty()) RequestListLoadStatus.EMPTY else RequestListLoadStatus.CONTENT
    } else state.status
    RequestListScreen(
        uiState = RequestListUiState(loadStatus, rows, sourceEmpty = data.sourceEmpty),
        onOpenRequest = onOpen,
        onBack = onBack,
        onRefresh = onRefresh,
        listState = list,
        isDemo = isDemo,
        hasUnreadResponses = canShowData && data.hasUnread,
        filters = {
            ResponseFilters(state.filter, data.forms,
                { onForm(it); list.requestScrollToItem(0) },
                { onPeriod(it); list.requestScrollToItem(0) })
            syncMessage?.let { Text(it) }
        },
        statusFilter = status.name,
        onStatusFilter = { onStatus(InboxFilter.fromKey(it)) },
        hasUnknownStatus = data.unknownStatusCount > 0,
        onForms = onForms,
        onlyUnread = onlyUnread,
        onOnlyUnreadChange = {
            onlyUnread = it
            list.requestScrollToItem(0)
        },
        hasActiveFilters = onlyUnread || status != InboxFilter.ALL ||
            state.filter != ResponseFilter(period = ResponsePeriod.ALL),
        onClearFilters = {
            onlyUnread = false
            onStatus(InboxFilter.ALL)
            onForm(null)
            onPeriod(ResponsePeriod.ALL)
            list.requestScrollToItem(0)
        },
        isRefreshing = state.isRefreshing,
        showCachedWarning = canShowData && state.status != RequestListLoadStatus.CONTENT
    )
}
