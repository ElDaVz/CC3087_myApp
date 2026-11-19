package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.ui.components.ResponseFilters
import com.uvg.cc3087.myapp.ui.components.ResponseLoadMessage
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
    val data = state.dashboard
    val rows = data.requests.filter { status.matches(it.requestStatus) }
    val loadStatus = if (state.hasData) {
        if (rows.isEmpty()) RequestListLoadStatus.EMPTY else RequestListLoadStatus.CONTENT
    } else state.status
    RequestListScreen(RequestListUiState(loadStatus, rows), onOpen, onBack, onRefresh,
        list, isDemo, hasUnreadResponses = data.hasUnread,
        filters = {
            ResponseFilters(state.filter, data.forms,
                { onForm(it); list.requestScrollToItem(0) },
                { onPeriod(it); list.requestScrollToItem(0) })
            syncMessage?.let { Text(it) }
            if (state.hasData && state.status != RequestListLoadStatus.CONTENT)
                ResponseLoadMessage(state.status, onRefresh)
        },
        statusFilter = status.name, onStatusFilter = { onStatus(InboxFilter.fromKey(it)) },
        hasUnknownStatus = data.unknownStatusCount > 0, onForms = onForms)
}
