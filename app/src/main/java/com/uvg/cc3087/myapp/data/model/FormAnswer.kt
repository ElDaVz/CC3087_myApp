package com.uvg.cc3087.myapp.data.model

import java.io.Serializable
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** Temporary respondent input; never part of the editable form definition. */
data class FormAnswer(
    val text: String = "",
    val selectedIds: Set<String> = emptySet(),
    val checked: Boolean = false,
    val date: String = "",
    val time: String = ""
) : Serializable

/** Language-independent validation results, translated by the UI. */
enum class FieldError {
    REQUIRED, INVALID_SELECTION, SINGLE_SELECTION, INVALID_DATE, INVALID_TIME, INCOMPLETE_DATE_TIME,
    PAST_DATE, INVALID_NUMBER, INTEGER_REQUIRED, BELOW_MINIMUM, ABOVE_MAXIMUM, INVALID_EMAIL,
    MISSING_QUESTION, TOO_FEW_OPTIONS, BLANK_OPTION, DUPLICATE_OPTIONS,
    INVALID_MINIMUM, INVALID_MAXIMUM, INVALID_RANGE
}

// No grouping or exponent notation: a quantity should never silently change its magnitude.
fun parseNumber(text: String, locale: Locale = Locale.getDefault()): BigDecimal? {
    val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val canonical = text.trim().replace(separator, '.')
    if (!Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)").matches(canonical)) return null
    return canonical.toBigDecimalOrNull()
}

fun FormFieldDraft.answerError(
    answer: FormAnswer,
    today: LocalDate = LocalDate.now(),
    locale: Locale = Locale.getDefault()
): FieldError? {
    val missing = when (type) {
        FormFieldType.CHECKBOX -> !answer.checked
        FormFieldType.MULTIPLE_CHOICE, FormFieldType.CHECKBOX_GROUP, FormFieldType.DROPDOWN ->
            answer.selectedIds.isEmpty()
        FormFieldType.DATE -> answer.date.isBlank() && (!includeTime || answer.time.isBlank())
        else -> answer.text.isBlank()
    }
    if (missing) return if (required) FieldError.REQUIRED else null
    if (type.hasOptions) {
        if (answer.selectedIds.any { id -> options.none { it.id == id } }) return FieldError.INVALID_SELECTION
        if (!isMultipleSelection && answer.selectedIds.size > 1) return FieldError.SINGLE_SELECTION
    }
    if (type == FormFieldType.DATE) {
        if (includeTime && (answer.date.isBlank() || answer.time.isBlank())) return FieldError.INCOMPLETE_DATE_TIME
        val date = runCatching { LocalDate.parse(answer.date) }.getOrNull() ?: return FieldError.INVALID_DATE
        if (includeTime && runCatching { LocalTime.parse(answer.time) }.isFailure) return FieldError.INVALID_TIME
        if (disallowPastDates && date < today) return FieldError.PAST_DATE
    }
    val text = answer.text.trim()
    if (type == FormFieldType.NUMBER) {
        val number = parseNumber(text, locale) ?: return FieldError.INVALID_NUMBER
        if (!allowDecimals && number.stripTrailingZeros().scale() > 0) return FieldError.INTEGER_REQUIRED
        parseNumber(minimum, Locale.ROOT)?.let { if (number < it) return FieldError.BELOW_MINIMUM }
        parseNumber(maximum, Locale.ROOT)?.let { if (number > it) return FieldError.ABOVE_MAXIMUM }
    }
    if (type == FormFieldType.EMAIL && !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(text)) {
        return FieldError.INVALID_EMAIL
    }
    return null
}
