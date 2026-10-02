package com.uvg.cc3087.myapp

import com.uvg.cc3087.myapp.data.local.FullFormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.FormStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormRepositoryTest {

    @Test
    fun testFormDraftCreationAndFields() {
        val fields = listOf(
            FormFieldDraft(id = "f1", type = FormFieldType.TEXT, title = "Nombre"),
            FormFieldDraft(id = "f2", type = FormFieldType.CHECKBOX_GROUP, title = "Opciones", allowMultipleAnswers = false)
        )
        val draft = FullFormDraft(
            id = "test-form-1",
            title = "Formulario de prueba",
            status = FormStatus.DRAFT,
            ownerId = "user-123",
            fields = fields
        )

        assertEquals("test-form-1", draft.id)
        assertEquals("Formulario de prueba", draft.title)
        assertEquals(FormStatus.DRAFT, draft.status)
        assertEquals(2, draft.fields.size)
        assertEquals(false, draft.fields[1].allowMultipleAnswers)
    }

    @Test
    fun testFormFieldSerializationData() {
        val field = FormFieldDraft(
            id = "field-check",
            type = FormFieldType.CHECKBOX_GROUP,
            title = "Seleccione una o varias",
            required = true,
            allowMultipleAnswers = true
        )
        assertTrue(field.required)
        assertTrue(field.allowMultipleAnswers)
        assertEquals(FormFieldType.CHECKBOX_GROUP, field.type)
    }
}
