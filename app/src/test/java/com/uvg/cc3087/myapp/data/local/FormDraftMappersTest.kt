package com.uvg.cc3087.myapp.data.local

import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import org.junit.Assert.assertEquals
import org.junit.Test

class FormDraftMappersTest {
    @Test
    fun mappingBackRestoresTheSavedPositionsInsteadOfTheDatabaseReturnOrder() {
        val draft = draft()
        val stored = FormDraftWithFields(draft.toEntity(), draft.toFieldEntities().reversed())

        assertEquals(draft, stored.toDraft())
    }

    @Test
    fun mappingTakesACopyOfMutableOptions() {
        val options = mutableListOf(FieldOption("a", "Café"))
        val draft = draft().copy(fields = listOf(draft().fields.first().copy(options = options)))
        val stored = draft.toFieldEntities()
        options.clear()

        assertEquals(listOf(FieldOption("a", "Café")), stored.single().options)
    }

    private fun draft() = FormDraft(
        id = "order", title = "Pedido", createdAt = 100, updatedAt = 200,
        fields = listOf(
            FormFieldDraft("name", FormFieldType.TEXT, "Nombre"),
            FormFieldDraft("date", FormFieldType.DATE, "Fecha", includeTime = false)
        )
    )
}
