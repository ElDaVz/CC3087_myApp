package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.RequestSummary

enum class RequestListLoadStatus { LOADING, CONTENT, EMPTY, FORBIDDEN, UNAVAILABLE, ERROR }

data class RequestListUiState(
    val status: RequestListLoadStatus = RequestListLoadStatus.LOADING,
    val requests: List<RequestSummary> = emptyList()
)
