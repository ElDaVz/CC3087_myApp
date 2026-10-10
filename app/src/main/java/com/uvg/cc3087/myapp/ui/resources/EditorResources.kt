package com.uvg.cc3087.myapp.ui.resources

import androidx.annotation.StringRes
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult

// estas etiquetas pertenecen a la ui, así el modelo no carga con un idioma fijo
@get:StringRes
val FormFieldType.labelResId: Int
    get() = when (this) {
        FormFieldType.TEXT -> R.string.editor_type_text
        FormFieldType.MULTIPLE_CHOICE -> R.string.editor_type_multiple_choice
        FormFieldType.DATE -> R.string.editor_type_date
    }

@get:StringRes
val FormFieldType.defaultTitleResId: Int
    get() = when (this) {
        FormFieldType.TEXT -> R.string.editor_default_text_title
        FormFieldType.MULTIPLE_CHOICE -> R.string.editor_default_choice_title
        FormFieldType.DATE -> R.string.editor_default_date_title
    }

@get:StringRes
val FormFieldType.addActionResId: Int
    get() = when (this) {
        FormFieldType.TEXT -> R.string.editor_add_text
        FormFieldType.MULTIPLE_CHOICE -> R.string.editor_add_multiple_choice
        FormFieldType.DATE -> R.string.editor_add_date
    }

// la validación devuelve un resultado y solo aquí elegimos su mensaje visible
@get:StringRes
val FormValidationResult.messageResId: Int
    get() = when (this) {
        FormValidationResult.TITLE_REQUIRED -> R.string.editor_title_required
        FormValidationResult.FIELD_REQUIRED -> R.string.editor_field_required
        FormValidationResult.FIELD_TITLE_REQUIRED -> R.string.editor_field_title_required
        FormValidationResult.READY -> R.string.editor_ready
    }
