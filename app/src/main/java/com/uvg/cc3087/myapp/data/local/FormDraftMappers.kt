package com.uvg.cc3087.myapp.data.local

import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft

internal fun FormDraft.toEntity() = FormDraftEntity(id, title, createdAt, updatedAt)

internal fun FormDraft.toFieldEntities(): List<FormFieldEntity> = fields.mapIndexed { index, field ->
    FormFieldEntity(
        formId = id, id = field.id, position = index, type = field.type, title = field.title,
        required = field.required, allowMultipleAnswers = field.allowMultipleAnswers,
        options = field.options.toList(), helpText = field.helpText, placeholder = field.placeholder,
        includeTime = field.includeTime, disallowPastDates = field.disallowPastDates,
        allowDecimals = field.allowDecimals, minimum = field.minimum,
        maximum = field.maximum, unit = field.unit
    )
}

internal fun FormDraftWithFields.toDraft() = FormDraft(
    id = form.id,
    title = form.title,
    createdAt = form.createdAt,
    updatedAt = form.updatedAt,
    // las relaciones de room no garantizan el orden de los campos
    fields = fields.sortedBy { it.position }.map { field ->
        FormFieldDraft(
            id = field.id, type = field.type, title = field.title, required = field.required,
            allowMultipleAnswers = field.allowMultipleAnswers, options = field.options.toList(),
            helpText = field.helpText, placeholder = field.placeholder,
            includeTime = field.includeTime, disallowPastDates = field.disallowPastDates,
            allowDecimals = field.allowDecimals, minimum = field.minimum,
            maximum = field.maximum, unit = field.unit
        )
    }
)
