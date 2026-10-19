package com.uvg.cc3087.myapp

import com.uvg.cc3087.myapp.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class FormAnswerTest {
    @Test fun requiredFieldsRejectEmptyAnswersForEveryType() {
        FormFieldType.entries.forEach { type ->
            assertNotNull(type.name, FormFieldDraft("id", type, "Question", required = true).answerError(FormAnswer()))
            assertNull(type.name, FormFieldDraft("id", type, "Question").answerError(FormAnswer()))
        }
    }

    @Test fun optionalTypedAnswersStillValidateWhenProvided() {
        val email = FormFieldDraft("email", FormFieldType.EMAIL, "Email")
        assertNotNull(email.answerError(FormAnswer(text = "not-an-email")))
        assertNull(email.answerError(FormAnswer(text = "person@example.com")))
        val number = email.copy(type = FormFieldType.NUMBER)
        assertNotNull(number.answerError(FormAnswer(text = "twelve")))
        assertNull(number.answerError(FormAnswer(text = "-12,5"), locale = Locale.forLanguageTag("es")))
        assertNull(number.answerError(FormAnswer(text = "-12.5"), locale = Locale.US))
    }

    @Test fun choicesRejectStaleOptionsAndRespectSingleAnswerMode() {
        val field = FormFieldDraft("choice", FormFieldType.CHECKBOX_GROUP, "Choice",
            options = listOf(FieldOption("a", "A"), FieldOption("b", "B")))
        val both = FormAnswer(selectedIds = setOf("a", "b"))
        assertNull(field.answerError(both))
        assertNotNull(field.copy(allowMultipleAnswers = false).answerError(both))
        assertNotNull(field.copy(type = FormFieldType.MULTIPLE_CHOICE).answerError(both))
        assertNotNull(field.copy(type = FormFieldType.DROPDOWN, allowMultipleAnswers = false).answerError(both))
        assertNull(field.copy(type = FormFieldType.DROPDOWN, allowMultipleAnswers = true).answerError(both))
        assertNotNull(field.answerError(FormAnswer(selectedIds = setOf("deleted"))))
    }

    @Test fun dateAndTimeMustBeCompletedTogether() {
        val field = FormFieldDraft("date", FormFieldType.DATE, "Appointment")
        assertNotNull(field.answerError(FormAnswer(date = "2026-10-08")))
        assertNotNull(field.answerError(FormAnswer(time = "14:30")))
        assertNull(field.answerError(FormAnswer(date = "2026-10-08", time = "14:30")))
    }
}
