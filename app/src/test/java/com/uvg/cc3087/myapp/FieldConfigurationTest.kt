package com.uvg.cc3087.myapp

import com.uvg.cc3087.myapp.data.model.*
import org.junit.Assert.*
import org.junit.Test

class FieldConfigurationTest {
    @Test fun selectionFieldsRequireTwoNonBlankOptions() {
        listOf(FormFieldType.MULTIPLE_CHOICE, FormFieldType.CHECKBOX_GROUP, FormFieldType.DROPDOWN).forEach { type ->
            val field = FormFieldDraft("test", type, "Question")
            assertTrue(field.hasValidOptions())
            assertFalse(field.copy(options = emptyList()).hasValidOptions())
            assertFalse(field.copy(options = field.options.take(1)).hasValidOptions())
            assertFalse(field.copy(options = listOf(FieldOption(label = "Yes"), FieldOption(label = " "))).hasValidOptions())
        }
    }

    @Test fun editingOptionLabelsPreservesIdentity() {
        val field = FormFieldDraft("test", FormFieldType.MULTIPLE_CHOICE, "Question")
        val option = field.options.first()
        assertEquals(option.id, option.copy(label = "Changed").id)
        assertNotEquals(field.options[0].id, field.options[1].id)
    }

    @Test fun nonSelectionFieldsDoNotRequireOptions() {
        FormFieldType.entries.filterNot { it.hasOptions }.forEach { type ->
            val field = FormFieldDraft("test", type, "Question")
            assertTrue(field.options.isEmpty())
            assertTrue(field.hasValidOptions())
        }
    }
}
