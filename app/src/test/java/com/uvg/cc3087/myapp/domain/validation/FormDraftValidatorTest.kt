package com.uvg.cc3087.myapp.domain.validation

import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.FieldError
import com.uvg.cc3087.myapp.data.model.FormAnswer
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun previewConfigurationOnlyIncludesFieldsWithErrors() {
        val invalid = FormFieldDraft("choice", FormFieldType.DROPDOWN, "", options = emptyList())

        assertEquals(
            mapOf("choice" to setOf(FieldError.MISSING_QUESTION, FieldError.TOO_FEW_OPTIONS)),
            FormDraftValidator.configurationErrors(listOf(validField, invalid))
        )
    }

    @Test
    fun previewDoesNotShowAnswerErrorsBeforeAnAttempt() {
        assertTrue(FormDraftValidator.validateAnswers(
            listOf(validField.copy(required = true)), emptyMap(), false,
            LocalDate.of(2026, 10, 26), Locale.US
        ).isEmpty())
    }

    @Test
    fun previewUsesFieldIdentityAndIgnoresAnswersForRemovedFields() {
        assertEquals(
            mapOf("name" to FieldError.REQUIRED),
            FormDraftValidator.validateAnswers(
                listOf(validField.copy(required = true)),
                mapOf("removed" to FormAnswer(text = "Ana")), true,
                LocalDate.of(2026, 10, 26), Locale.US
            )
        )
    }

    @Test
    fun previewUsesTheSuppliedDateAndLocale() {
        val fields = listOf(
            FormFieldDraft("date", FormFieldType.DATE, "Fecha", includeTime = false, disallowPastDates = true),
            FormFieldDraft("number", FormFieldType.NUMBER, "Cantidad")
        )
        val answers = mapOf(
            "date" to FormAnswer(date = "2026-10-25"),
            "number" to FormAnswer(text = "1,5")
        )

        assertEquals(
            mapOf("date" to FieldError.PAST_DATE),
            FormDraftValidator.validateAnswers(fields, answers, true, LocalDate.of(2026, 10, 26), Locale.GERMANY)
        )
        assertTrue(FormDraftValidator.validateAnswers(
            fields, answers, true, LocalDate.of(2026, 10, 25), Locale.GERMANY
        ).isEmpty())
    }
}
