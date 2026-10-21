package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
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

    private fun model(repository: FakeFormRepository, saved: SavedStateHandle = SavedStateHandle()) =
        SaveShareViewModel(repository, saved).also { viewModels.put("receipt-${viewModelNumber++}", it) }
}
