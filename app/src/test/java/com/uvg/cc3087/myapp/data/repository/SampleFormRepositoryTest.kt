package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SampleFormRepositoryTest {
    @Test
    fun repeatedSubscriptionsKeepSampleIdsAndOrder() = runTest {
        val repository = SampleFormRepository()

        val firstList = repository.observeForms().first()
        val secondList = repository.observeForms().first()

        assertEquals(10, firstList.size)
        assertEquals("bakery-custom-order", firstList.first().id)
        assertEquals(firstList.map { it.id }, secondList.map { it.id })
        assertEquals(firstList.size, firstList.map { it.id }.distinct().size)
    }

    @Test
    fun translatedSampleDataIsPublishedWithoutChangingIds() = runTest {
        val repository = SampleFormRepository()
        val initial = repository.observeForms().first()
        repository.updateSampleForms(initial.map { it.copy(title = "Traducido: ${it.id}") })
        val updated = repository.observeForms().first()
        assertEquals(initial.map { it.id }, updated.map { it.id })
        assertEquals("Traducido: bakery-custom-order", updated.first().title)
    }

    @Test
    fun savingTheSameDraftUpdatesItInsteadOfAddingAnotherSummary() = runTest {
        val repository = SampleFormRepository()
        val draft = FormDraft("local", "Pedido", emptyList(), 100, 200)
        repository.saveDraft(draft)
        repository.saveDraft(draft.copy(title = "Editado", updatedAt = 300))

        assertEquals("Editado", repository.getDraft("local")?.title)
        assertEquals(1, repository.observeForms().first().count { it.id == "local" })
        repository.deleteDraft("local")
        assertNull(repository.getDraft("local"))
        assertEquals(10, repository.observeForms().first().size)
    }

    @Test
    fun translatingSamplesDoesNotReplaceTheUsersDraft() = runTest {
        val repository = SampleFormRepository()
        val draft = FormDraft("local", "Mi formulario", emptyList(), 100, 200)
        repository.saveDraft(draft)
        repository.updateSampleForms(emptyList())

        assertEquals(draft, repository.getDraft("local"))
        assertEquals(listOf("Mi formulario"), repository.observeForms().first().map { it.title })
    }

    @Test
    fun savingCopiesTheFieldsInsteadOfKeepingAMutableList() = runTest {
        val repository = SampleFormRepository()
        val fields = mutableListOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre"))
        repository.saveDraft(FormDraft("local", "Pedido", fields, 100, 200))
        fields.clear()

        assertEquals("Nombre", repository.getDraft("local")!!.fields.single().title)
    }
}
