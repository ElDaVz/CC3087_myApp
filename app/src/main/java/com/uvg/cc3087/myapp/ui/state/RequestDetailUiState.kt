package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.RequestDetail

enum class RequestDetailLoadStatus { IDLE, LOADING, CONTENT, NOT_FOUND, FORBIDDEN, UNAVAILABLE, ERROR }

data class RequestDetailUiState(
    val status: RequestDetailLoadStatus = RequestDetailLoadStatus.IDLE,
    val detail: RequestDetail? = null,
    val savingStatus: Boolean = false,
    val statusSaveFailed: Boolean = false
)
