package com.uvg.cc3087.myapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.FieldError
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import java.text.DecimalFormatSymbols
import java.util.Locale

@get:StringRes
val FormFieldType.labelRes: Int
    get() = when (this) {
        FormFieldType.TEXT -> R.string.field_text
        FormFieldType.PARAGRAPH -> R.string.field_paragraph
        FormFieldType.NUMBER -> R.string.field_number
        FormFieldType.EMAIL -> R.string.field_email
        FormFieldType.CHECKBOX -> R.string.field_confirmation
        FormFieldType.CHECKBOX_GROUP, FormFieldType.MULTIPLE_CHOICE -> R.string.field_selection
        FormFieldType.DROPDOWN -> R.string.field_dropdown
        FormFieldType.DATE -> R.string.field_date
    }

@Composable
fun currentFormLocale(): Locale = LocalConfiguration.current.locales[0]

fun localizedNumber(value: String, locale: Locale): String =
    value.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)

fun canonicalNumber(value: String, locale: Locale): String =
    value.replace(DecimalFormatSymbols.getInstance(locale).decimalSeparator, '.')

@Composable
fun fieldErrorText(error: FieldError, field: FormFieldDraft): String {
    val res = when (error) {
        FieldError.REQUIRED -> R.string.error_required
        FieldError.INVALID_SELECTION -> R.string.error_invalid_selection
        FieldError.SINGLE_SELECTION -> R.string.error_single_selection
        FieldError.INVALID_DATE -> R.string.error_invalid_date
        FieldError.INVALID_TIME -> R.string.error_invalid_time
        FieldError.INCOMPLETE_DATE_TIME -> R.string.error_date_time
        FieldError.PAST_DATE -> R.string.error_past_date
        FieldError.INVALID_NUMBER -> R.string.error_number
        FieldError.INTEGER_REQUIRED -> R.string.error_integer
        FieldError.BELOW_MINIMUM -> R.string.error_below_minimum
        FieldError.ABOVE_MAXIMUM -> R.string.error_above_maximum
        FieldError.INVALID_EMAIL -> R.string.error_email
        FieldError.MISSING_QUESTION -> R.string.error_question
        FieldError.TOO_FEW_OPTIONS -> R.string.error_options_count
        FieldError.BLANK_OPTION -> R.string.error_option_blank
        FieldError.DUPLICATE_OPTIONS -> R.string.error_options_duplicate
        FieldError.INVALID_MINIMUM -> R.string.error_minimum
        FieldError.INVALID_MAXIMUM -> R.string.error_maximum
        FieldError.INVALID_RANGE -> R.string.error_range
    }
    return when (error) {
        FieldError.BELOW_MINIMUM -> stringResource(res, localizedNumber(field.minimum, currentFormLocale()))
        FieldError.ABOVE_MAXIMUM -> stringResource(res, localizedNumber(field.maximum, currentFormLocale()))
        else -> stringResource(res)
    }
}
