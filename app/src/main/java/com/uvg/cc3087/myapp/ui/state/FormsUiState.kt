package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.FormSummary

// la pantalla recibe la lista ya filtrada y la opción que debe marcar
data class FormsUiState(
    val forms: List<FormSummary> = emptyList(),
    val selectedFilter: FormFilter = FormFilter.ALL,
    val selectedSort: FormSortOrder = FormSortOrder.RECENT_FIRST,
    val status: FormsLoadStatus = FormsLoadStatus.LOADING,
    val deletion: FormDeletionState = FormDeletionState(),
    val searchQuery: String = "",
    val searchExpanded: Boolean = false,
    val emptyReason: FormsEmptyReason = FormsEmptyReason.SOURCE
)

enum class FormsLoadStatus { LOADING, EMPTY, CONTENT, ERROR }

enum class FormsEmptyReason { SOURCE, FILTER, SEARCH }

data class FormDeletionState(
    val target: FormSummary? = null,
    val inProgress: Boolean = false,
    val failed: Boolean = false,
    val completed: Boolean = false
)
