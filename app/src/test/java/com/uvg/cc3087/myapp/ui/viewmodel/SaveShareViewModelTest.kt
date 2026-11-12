package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.repository.PublicationError
import com.uvg.cc3087.myapp.data.repository.PublicationException
import com.uvg.cc3087.myapp.testing.FakeFormRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.DraftLoadError
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveShareViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val viewModels = ViewModelStore()
    private var viewModelNumber = 0
    private val draft = FormDraft(
        "stored", "Pedido", listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre")), 100, 200
    )

    @After fun clearViewModels() { viewModels.clear() }

    @Test fun openingAReceiptReadsTheStoredDraftWithoutWritingOrInventingALink() = runTest {
        val repository = FakeFormRepository(listOf(draft))
        val model = model(repository)
        model.openDraft(draft.id)
        assertTrue(model.uiState.value.isLoading)
        runCurrent()

        assertEquals(draft, model.uiState.value.draft)
        assertFalse(model.uiState.value.isLoading)
        assertFalse(model.uiState.value.canShare)
        assertNull(model.uiState.value.publicLink)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test fun aMissingDraftDoesNotShowASuccessReceipt() = runTest {
        val model = model(FakeFormRepository())
        model.openDraft("missing")
        runCurrent()

        assertNull(model.uiState.value.draft)
        assertEquals(DraftLoadError.NOT_FOUND, model.uiState.value.loadError)
    }

    @Test fun retryReadsAgainAfterAStorageError() = runTest {
        val repository = FakeFormRepository(listOf(draft)).apply { loadFailure = IOException("read failed") }
        val model = model(repository)
        model.openDraft(draft.id)
        runCurrent()
        assertEquals(DraftLoadError.FAILED, model.uiState.value.loadError)
        repository.loadFailure = null
        model.retryLoading()
        runCurrent()

        assertEquals(draft, model.uiState.value.draft)
        assertNull(model.uiState.value.loadError)
    }

    @Test fun androidRestorationLoadsTheSameIdFromTheRepository() = runTest {
        val saved = SavedStateHandle()
        val repository = FakeFormRepository(listOf(draft))
        model(repository, saved).openDraft(draft.id)
        runCurrent()
        val values = saved.keys().associateWith { saved.get<Any?>(it) }
        val restored = model(repository, SavedStateHandle(values))
        runCurrent()

        assertEquals(draft, restored.uiState.value.draft)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test fun openingAnotherDraftCancelsTheOlderRequest() = runTest {
        val other = draft.copy(id = "other", title = "Otro")
        val repository = FakeFormRepository(listOf(draft, other)).apply { loadDelayMillis = 100 }
        val model = model(repository)
        model.openDraft(draft.id)
        runCurrent()
        model.openDraft(other.id)
        advanceUntilIdle()

        assertEquals(other, model.uiState.value.draft)
    }

    @Test fun publicationWaitsForBackendAndIgnoresDoubleTap() = runTest {
        val pending = kotlinx.coroutines.CompletableDeferred<com.uvg.cc3087.myapp.data.repository.FormPublication>()
        var calls = 0
        val repository = object : com.uvg.cc3087.myapp.data.repository.FormRepository by FakeFormRepository(listOf(draft)) {
            override suspend fun publish(id: String): com.uvg.cc3087.myapp.data.repository.FormPublication { calls++; return pending.await() }
        }
        val model = model(repository); model.openDraft(draft.id); runCurrent()
        model.publish(); model.publish(); runCurrent()
        assertTrue(model.uiState.value.publishing); assertFalse(model.uiState.value.canShare); assertEquals(1, calls)
        pending.complete(com.uvg.cc3087.myapp.data.repository.FormPublication("https://example.web.app/f/stored", "PUBLISHED", 1, 100, true)); runCurrent()
        assertTrue(model.uiState.value.canShare); assertFalse(model.uiState.value.publishing)
    }
    @Test fun failedPublicationRemainsRetryableWithoutInventingALink() = runTest {
        val repository = object : com.uvg.cc3087.myapp.data.repository.FormRepository by FakeFormRepository(listOf(draft)) {
            override suspend fun publish(id: String): com.uvg.cc3087.myapp.data.repository.FormPublication = throw IOException("network")
        }
        val model = model(repository); model.openDraft(draft.id); runCurrent(); model.publish(); runCurrent()
        assertEquals(PublicationError.PUBLISH_FAILED, model.uiState.value.publishError)
        assertFalse(model.uiState.value.canShare); assertFalse(model.uiState.value.publishing)
    }
    @Test fun publicationFailureKeepsItsReasonForTheUiToTranslate() = runTest {
        val repository = object : com.uvg.cc3087.myapp.data.repository.FormRepository by FakeFormRepository(listOf(draft)) {
            override suspend fun publish(id: String): com.uvg.cc3087.myapp.data.repository.FormPublication =
                throw PublicationException(PublicationError.COVER_UPLOAD_FAILED)
        }
        val model = model(repository)
        model.openDraft(draft.id); runCurrent(); model.publish(); runCurrent()
        assertEquals(PublicationError.COVER_UPLOAD_FAILED, model.uiState.value.publishError)
    }
    @Test fun archiveFailureReturnsATranslatableReason() = runTest {
        val repository = object : com.uvg.cc3087.myapp.data.repository.FormRepository by FakeFormRepository(listOf(draft)) {
            override suspend fun archive(id: String) { throw IOException("network") }
        }
        val model = model(repository)
        model.openDraft(draft.id); runCurrent(); model.archive(); runCurrent()
        assertEquals(PublicationError.ARCHIVE_FAILED, model.uiState.value.publishError)
        assertFalse(model.uiState.value.publishing)
    }
    @Test fun aLatePublicationDoesNotAttachItsLinkToAnotherDraft() = runTest {
        val pending = kotlinx.coroutines.CompletableDeferred<com.uvg.cc3087.myapp.data.repository.FormPublication>()
        val other = draft.copy(id = "other")
        val repository = object : com.uvg.cc3087.myapp.data.repository.FormRepository by FakeFormRepository(listOf(draft, other)) {
            override suspend fun publish(id: String) = pending.await()
        }
        val model = model(repository); model.openDraft(draft.id); runCurrent(); model.publish(); runCurrent()
        model.openDraft(other.id); runCurrent()
        pending.complete(com.uvg.cc3087.myapp.data.repository.FormPublication("https://example.web.app/f/stored", "PUBLISHED", 1, 100, true)); runCurrent()
        assertEquals("other", model.uiState.value.draft!!.id); assertFalse(model.uiState.value.canShare)
    }

    private fun model(repository: com.uvg.cc3087.myapp.data.repository.FormRepository, saved: SavedStateHandle = SavedStateHandle()) =
        SaveShareViewModel(repository, saved).also { viewModels.put("receipt-${viewModelNumber++}", it) }
}
