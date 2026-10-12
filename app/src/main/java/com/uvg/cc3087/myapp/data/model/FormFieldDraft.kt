package com.uvg.cc3087.myapp.data.model

import java.io.Serializable

enum class FormFieldType {
    TEXT, PARAGRAPH, NUMBER, EMAIL, CHECKBOX, CHECKBOX_GROUP, DROPDOWN, MULTIPLE_CHOICE, DATE
}

data class FieldOption(val id: String = java.util.UUID.randomUUID().toString(), val label: String) : Serializable

val FormFieldType.hasOptions: Boolean
    get() = this in listOf(FormFieldType.MULTIPLE_CHOICE, FormFieldType.CHECKBOX_GROUP, FormFieldType.DROPDOWN)

data class FormFieldDraft(
    val id: String,
    val type: FormFieldType,
    val title: String,
    val required: Boolean = false,
    val allowMultipleAnswers: Boolean = type == FormFieldType.CHECKBOX_GROUP,
    val options: List<FieldOption> = if (type.hasOptions) listOf(
        FieldOption(label = ""), FieldOption(label = "")
    ) else emptyList(),
    val helpText: String = "",
    val placeholder: String = "",
    // Existing date fields keep their time input; new fields explicitly default to date-only.
    val includeTime: Boolean = true,
    val disallowPastDates: Boolean = false,
    val allowDecimals: Boolean = true,
    // Bounds use canonical decimal notation, retaining invalid draft input for inline feedback.
    val minimum: String = "",
    val maximum: String = "",
    val unit: String = ""
) : Serializable

val FormFieldDraft.isMultipleSelection: Boolean
    get() = type.hasOptions && type != FormFieldType.MULTIPLE_CHOICE && allowMultipleAnswers

fun FormFieldDraft.withSelectionMode(multiple: Boolean): FormFieldDraft = copy(
    type = if (type == FormFieldType.DROPDOWN) type
        else if (multiple) FormFieldType.CHECKBOX_GROUP else FormFieldType.MULTIPLE_CHOICE,
    allowMultipleAnswers = multiple
)

fun FormFieldDraft.optionError(): FieldError? = when {
    !type.hasOptions -> null
    options.size < 2 -> FieldError.TOO_FEW_OPTIONS
    options.any { it.label.isBlank() } -> FieldError.BLANK_OPTION
    options.map { it.label.trim().lowercase(java.util.Locale.ROOT) }.distinct().size != options.size ->
        FieldError.DUPLICATE_OPTIONS
    else -> null
}

fun FormFieldDraft.hasValidOptions(): Boolean = optionError() == null

fun FormFieldDraft.configurationErrors(): Set<FieldError> = buildSet {
    if (title.isBlank()) add(FieldError.MISSING_QUESTION)
    optionError()?.let(::add)
    if (type == FormFieldType.NUMBER) {
        val min = parseNumber(minimum, java.util.Locale.ROOT)
        val max = parseNumber(maximum, java.util.Locale.ROOT)
        if (minimum.isNotBlank() && (min == null || (!allowDecimals && min.stripTrailingZeros().scale() > 0))) {
            add(FieldError.INVALID_MINIMUM)
        }
        if (maximum.isNotBlank() && (max == null || (!allowDecimals && max.stripTrailingZeros().scale() > 0))) {
            add(FieldError.INVALID_MAXIMUM)
        }
        if (min != null && max != null && min > max) add(FieldError.INVALID_RANGE)
    }
}
