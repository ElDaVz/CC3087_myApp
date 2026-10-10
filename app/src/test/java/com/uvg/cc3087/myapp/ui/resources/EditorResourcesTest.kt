package com.uvg.cc3087.myapp.ui.resources

import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import org.junit.Assert.assertEquals
import org.junit.Test

// comprobamos los ids de presentación sin pedir recursos a un teléfono
class EditorResourcesTest {
    @Test
    fun fieldTypesUseTheirOwnLabelResources() {
        assertEquals(
            listOf(R.string.editor_type_text, R.string.editor_type_multiple_choice, R.string.editor_type_date),
            FormFieldType.entries.map { it.labelResId }
        )
    }

    @Test
    fun initialTitlesMatchEachFieldType() {
        assertEquals(
            listOf(
                R.string.editor_default_text_title,
                R.string.editor_default_choice_title,
                R.string.editor_default_date_title
            ),
            FormFieldType.entries.map { it.defaultTitleResId }
        )
    }

    @Test
    fun addActionsMatchEachFieldType() {
        assertEquals(
            listOf(R.string.editor_add_text, R.string.editor_add_multiple_choice, R.string.editor_add_date),
            FormFieldType.entries.map { it.addActionResId }
        )
    }

    @Test
    fun validationResultsUseTheCorrectMessageResources() {
        assertEquals(
            listOf(
                R.string.editor_title_required,
                R.string.editor_field_required,
                R.string.editor_field_title_required,
                R.string.editor_ready
            ),
            FormValidationResult.entries.map { it.messageResId }
        )
    }
}
