package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.FormFieldDraft

data class EditFormUiState(
    val title: String = "",
    val fields: List<FormFieldDraft> = emptyList()
) {
    val hasTitleError: Boolean
        get() = title.isBlank()

    val fieldIdsWithTitleErrors: Set<String>
        get() = fields.filter { it.title.isBlank() }.map { it.id }.toSet()
}
