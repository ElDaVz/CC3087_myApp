package com.uvg.cc3087.myapp.domain.validation

import com.uvg.cc3087.myapp.data.model.FormFieldDraft

enum class FormValidationResult {
    TITLE_REQUIRED,
    FIELD_REQUIRED,
    FIELD_TITLE_REQUIRED,
    READY
}

// mantenemos el orden de los avisos que ya mostraba el editor
object FormDraftValidator {
    fun validate(title: String, fields: List<FormFieldDraft>): FormValidationResult = when {
        title.isBlank() -> FormValidationResult.TITLE_REQUIRED
        fields.isEmpty() -> FormValidationResult.FIELD_REQUIRED
        fields.any { it.title.isBlank() } -> FormValidationResult.FIELD_TITLE_REQUIRED
        else -> FormValidationResult.READY
    }
}
