package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.FormFieldDraft

enum class DraftSaveStatus { IDLE, SAVING, SAVED, ERROR }
enum class DraftLoadError { NOT_FOUND, FAILED }

data class EditFormUiState(
    val title: String = "",
    val fields: List<FormFieldDraft> = emptyList(),
    val draftId: String? = null,
    val isLoading: Boolean = false,
    val loadError: DraftLoadError? = null,
    val saveStatus: DraftSaveStatus = DraftSaveStatus.IDLE
) {
    val hasTitleError: Boolean
        get() = title.isBlank()

    val fieldIdsWithTitleErrors: Set<String>
        get() = fields.filter { it.title.isBlank() }.map { it.id }.toSet()
}
