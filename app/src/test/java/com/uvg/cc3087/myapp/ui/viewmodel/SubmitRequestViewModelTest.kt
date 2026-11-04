package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class SubmitRequestViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val store = ViewModelStore()
    private val form = FormDraft("f", "Pedido", listOf(
        FormFieldDraft("name", FormFieldType.TEXT, "Nombre", required = true)
    ), 1, 1)
    private val detail = RequestDetail("f", "r", "Pedido", null, emptyList(), requestStatus = RequestStatus.PENDING)
    private val forms = object : FormRepository {
        override fun observeForms() = flowOf(emptyList<FormSummary>())
        override suspend fun getDraft(id: String) = form
        override suspend fun saveDraft(draft: FormDraft) = Unit
        override suspend fun deleteDraft(id: String) = Unit
    }
    @After fun cleanup() { store.clear() }
    private fun model(repo: RequestRepository) = SubmitRequestViewModel(forms, repo, "f", "r").also { store.put("submission", it) }

    @Test fun captureUsesCentralValidationAndDoesNotPersistWhileValidating() = runTest {
        val model = model(RequestRepository { _, _ -> null })
        runCurrent()
        assertEquals(FormValidationResult.READY, model.validationResult())
        assertTrue(model.configurationErrors().isEmpty())
        assertTrue(model.validateAnswers(emptyMap(), false, Locale.US).isEmpty())
        assertEquals(mapOf("name" to FieldError.REQUIRED), model.validateAnswers(emptyMap(), true, Locale.US))
        assertTrue(model.validateAnswers(mapOf("name" to FormAnswer(text = "Ana")), true, Locale.US).isEmpty())
        assertFalse(model.uiState.value.saved)
        assertFalse(model.uiState.value.saving)
    }

    @Test fun doubleTapAndRetryUseOneStableRequestIdAndSuccessOnlyAfterPersistence() = runTest {
        val gate = CompletableDeferred<Unit>()
        val ids = mutableListOf<String>()
        var fail = true
        val model = model(object : RequestRepository {
            override suspend fun getDetail(formId: String, responseId: String): RequestDetail? = null
            override suspend fun submit(form: FormDraft, responseId: String, answers: Map<String, FormAnswer>, locale: Locale): RequestDetail {
                ids += responseId
                gate.await()
                if (fail) throw IOException()
                return detail
            }
        })
        runCurrent()
        model.submit(emptyMap(), Locale.US)
        model.submit(emptyMap(), Locale.US)
        runCurrent()
        assertEquals(listOf("r"), ids)
        assertTrue(model.uiState.value.saving)
        assertFalse(model.uiState.value.saved)
        gate.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.saveFailed)
        assertFalse(model.uiState.value.saved)
        fail = false
        model.submit(emptyMap(), Locale.US)
        runCurrent()
        assertEquals(listOf("r", "r"), ids)
        assertTrue(model.uiState.value.saved)
        model.submit(emptyMap(), Locale.US)
        runCurrent()
        assertEquals(2, ids.size)
    }

    @Test fun recreationDetectsCommittedRequestInsteadOfAllowingAnotherSubmit() = runTest {
        val model = model(RequestRepository { _, _ -> detail })
        runCurrent()
        assertTrue(model.uiState.value.saved)
        assertFalse(model.uiState.value.loading)
    }
}
