package com.uvg.cc3087.myapp.domain.validation

import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FieldError
import com.uvg.cc3087.myapp.data.model.FormAnswer
import com.uvg.cc3087.myapp.data.model.answerError
import com.uvg.cc3087.myapp.data.model.configurationErrors
import java.time.LocalDate
import java.util.Locale

enum class FormValidationResult {
    TITLE_REQUIRED,
    FIELD_REQUIRED,
    FIELD_TITLE_REQUIRED,
    FIELD_CONFIGURATION_INVALID,
    READY
}

// mantenemos el orden de los avisos que ya mostraba el editor
object FormDraftValidator {
    fun validate(title: String, fields: List<FormFieldDraft>): FormValidationResult = when {
        title.isBlank() -> FormValidationResult.TITLE_REQUIRED
        fields.isEmpty() -> FormValidationResult.FIELD_REQUIRED
        fields.any { it.title.isBlank() } -> FormValidationResult.FIELD_TITLE_REQUIRED
        fields.any { it.configurationErrors().isNotEmpty() } -> FormValidationResult.FIELD_CONFIGURATION_INVALID
        else -> FormValidationResult.READY
    }

    fun configurationErrors(fields: List<FormFieldDraft>): Map<String, Set<FieldError>> = buildMap {
        fields.forEach { field ->
            val errors = field.configurationErrors()
            if (errors.isNotEmpty()) put(field.id, errors)
        }
    }

    fun validateAnswers(
        fields: List<FormFieldDraft>,
        answers: Map<String, FormAnswer>,
        attempted: Boolean,
        today: LocalDate,
        locale: Locale
    ): Map<String, FieldError> {
        if (!attempted) return emptyMap()
        return fields.mapNotNull { field ->
            field.answerError(answers[field.id] ?: FormAnswer(), today, locale)?.let { field.id to it }
        }.toMap()
    }
}
