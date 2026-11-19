package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.model.RequestStatus

enum class RequestListLoadStatus { LOADING, CONTENT, EMPTY, FORBIDDEN, UNAVAILABLE, ERROR }

data class RequestListUiState(
    val status: RequestListLoadStatus = RequestListLoadStatus.LOADING,
    val requests: List<RequestSummary> = emptyList(),
    val sourceEmpty: Boolean = true
) {
    val unreadCount: Int get() = requests.count { !it.isRead }
    val unknownStatusCount: Int get() = requests.count { it.requestStatus == null }
    val statusCounts: Map<RequestStatus, Int>
        get() = RequestStatus.entries.associateWith { status -> requests.count { it.requestStatus == status } }
}
