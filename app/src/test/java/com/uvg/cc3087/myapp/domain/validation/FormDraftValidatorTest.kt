package com.uvg.cc3087.myapp.domain.validation

import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.FieldOption
import org.junit.Assert.assertEquals
import org.junit.Test

class FormDraftValidatorTest {
    private val validField = FormFieldDraft("name", FormFieldType.TEXT, "Nombre")

    @Test
    fun missingTitleTakesPriorityOverMissingFields() {
        assertEquals(
            FormValidationResult.TITLE_REQUIRED,
            FormDraftValidator.validate("", emptyList())
        )
    }

    @Test
    fun whitespaceTitleIsRejectedEvenWithValidFields() {
        assertEquals(
            FormValidationResult.TITLE_REQUIRED,
            FormDraftValidator.validate("  \n ", listOf(validField))
        )
    }

    @Test
    fun atLeastOneFieldIsRequired() {
        assertEquals(
            FormValidationResult.FIELD_REQUIRED,
            FormDraftValidator.validate("Pedido", emptyList())
        )
    }

    @Test
    fun anEmptyFieldTitleIsRejected() {
        assertEquals(
            FormValidationResult.FIELD_TITLE_REQUIRED,
            FormDraftValidator.validate("Pedido", listOf(validField.copy(title = "")))
        )
    }

    @Test
    fun aWhitespaceFieldTitleIsRejected() {
        assertEquals(
            FormValidationResult.FIELD_TITLE_REQUIRED,
            FormDraftValidator.validate(
                "Pedido",
                listOf(validField, validField.copy(id = "other", title = "  "))
            )
        )
    }

    @Test
    fun aNamedFormWithNamedFieldsIsReady() {
        assertEquals(
            FormValidationResult.READY,
            FormDraftValidator.validate("Pedido", listOf(validField))
        )
    }

    @Test
    fun choicesNeedValidNonRepeatedOptions() {
        val choice = FormFieldDraft("choice", FormFieldType.DROPDOWN, "Bebida")
        assertEquals(FormValidationResult.FIELD_CONFIGURATION_INVALID,
            FormDraftValidator.validate("Pedido", listOf(choice)))
        assertEquals(FormValidationResult.READY, FormDraftValidator.validate("Pedido", listOf(
            choice.copy(options = listOf(FieldOption("a", "Café"), FieldOption("b", "Té")))
        )))
    }

    @Test
    fun numberBoundsAreValidatedBeforeTheFormIsReady() {
        val number = FormFieldDraft("number", FormFieldType.NUMBER, "Cantidad",
            minimum = "10", maximum = "2")
        assertEquals(FormValidationResult.FIELD_CONFIGURATION_INVALID,
            FormDraftValidator.validate("Pedido", listOf(number)))
        assertEquals(FormValidationResult.READY,
            FormDraftValidator.validate("Pedido", listOf(number.copy(maximum = "20"))))
    }
}
