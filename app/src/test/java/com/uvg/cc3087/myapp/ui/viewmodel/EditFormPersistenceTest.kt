package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.testing.FakeFormRepository
import com.uvg.cc3087.myapp.testing.FakeTemplateRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.DraftLoadError
import com.uvg.cc3087.myapp.ui.state.DraftSaveStatus
import com.uvg.cc3087.myapp.ui.state.EditFormAction
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditFormPersistenceTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val viewModels = ViewModelStore()
    private var viewModelNumber = 0
    private var time = 100L

    @After fun clearViewModels() { viewModels.clear() }

    @Test fun enteringTheAppDoesNotCreateAnEmptyDraft() = runTest {
        val repository = FakeFormRepository()
        val editor = editor(repository)
        runCurrent()

        assertNull(editor.uiState.value.draftId)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test fun editsAreSavedWithTheSameIdentityAndOriginalCreationTime() = runTest {
        val repository = FakeFormRepository()
        val editor = editor(repository)
        editor.startDraft("Pedido", null)
        runCurrent()
        val original = repository.savedDrafts.single()
        time = 200
        editor.onAction(EditFormAction.ChangeTitle("Pedido actualizado"))
        editor.onAction(EditFormAction.AddField(FormFieldType.NUMBER, "Cantidad"))
        runCurrent()

        val saved = repository.getDraft(original.id)!!
        assertEquals(original.id, saved.id)
        assertEquals(100, saved.createdAt)
        assertEquals(200, saved.updatedAt)
        assertEquals("Pedido actualizado", saved.title)
        assertEquals(editor.uiState.value.fields, saved.fields)
        assertEquals(DraftSaveStatus.SAVED, editor.uiState.value.saveStatus)
    }

    @Test fun rapidEditsWaitForEarlierWritesAndOnlyTheLatestCanReportSaved() = runTest {
        val repository = FakeFormRepository().apply { saveDelayMillis = 100 }
        val editor = editor(repository)
        editor.startDraft("Uno", null)
        editor.onAction(EditFormAction.ChangeTitle("Dos"))
        editor.onAction(EditFormAction.ChangeTitle("Tres"))
        advanceTimeBy(100)
        runCurrent()
        assertEquals(DraftSaveStatus.SAVING, editor.uiState.value.saveStatus)
        advanceUntilIdle()

        assertEquals(listOf("Uno", "Dos", "Tres"), repository.savedDrafts.map { it.title })
        assertEquals("Tres", repository.getDraft(editor.uiState.value.draftId!!)!!.title)
        assertEquals(DraftSaveStatus.SAVED, editor.uiState.value.saveStatus)
    }

    @Test fun reopeningFromFormsWaitsForPendingEditsInsteadOfLoadingStaleContent() = runTest {
        val repository = FakeFormRepository().apply { saveDelayMillis = 100 }
        val editor = editor(repository)
        editor.startDraft("Antes", null)
        val id = editor.uiState.value.draftId!!
        editor.onAction(EditFormAction.ChangeTitle("Después"))
        editor.openDraft(id)
        assertTrue(editor.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(id, editor.uiState.value.draftId)
        assertEquals("Después", editor.uiState.value.title)
        assertEquals(2, repository.savedDrafts.size)
    }

    @Test fun openingAnExistingDraftDoesNotInsertItAgain() = runTest {
        val original = storedDraft()
        val repository = FakeFormRepository(listOf(original))
        val editor = editor(repository)
        editor.openDraft(original.id)
        runCurrent()

        assertEquals(original.fields, editor.uiState.value.fields)
        assertEquals(original.id, editor.uiState.value.draftId)
        assertTrue(repository.savedDrafts.isEmpty())
        editor.onAction(EditFormAction.ChangeTitle("Editado"))
        runCurrent()
        assertEquals(original.id, repository.savedDrafts.single().id)
        assertEquals(original.createdAt, repository.savedDrafts.single().createdAt)
    }

    @Test fun startingAnotherDraftDoesNotDropThePreviousDraftsPendingWrite() = runTest {
        val repository = FakeFormRepository().apply { saveDelayMillis = 100 }
        val editor = editor(repository)
        editor.startDraft("Primero", null)
        val firstId = editor.uiState.value.draftId!!
        editor.startDraft("Segundo", null)
        val secondId = editor.uiState.value.draftId!!
        advanceUntilIdle()

        assertNotEquals(firstId, secondId)
        assertEquals("Primero", repository.getDraft(firstId)!!.title)
        assertEquals("Segundo", repository.getDraft(secondId)!!.title)
    }

    @Test fun aSaveFailureKeepsTheEditsAndRetryUsesTheSameDraftId() = runTest {
        val repository = FakeFormRepository().apply { saveFailure = IOException("disk full") }
        val editor = editor(repository)
        editor.startDraft("Sin perder", null)
        runCurrent()
        val id = editor.uiState.value.draftId!!
        assertEquals(DraftSaveStatus.ERROR, editor.uiState.value.saveStatus)
        assertEquals("Sin perder", editor.uiState.value.title)
        repository.saveFailure = null
        editor.retrySave()
        runCurrent()

        assertEquals("Sin perder", repository.getDraft(id)!!.title)
        assertEquals(DraftSaveStatus.SAVED, editor.uiState.value.saveStatus)
    }

    @Test fun aMissingOrUnreadableDraftNeverTurnsIntoANewBlankForm() = runTest {
        val repository = FakeFormRepository()
        val editor = editor(repository)
        editor.openDraft("missing")
        runCurrent()
        assertEquals(DraftLoadError.NOT_FOUND, editor.uiState.value.loadError)
        editor.onAction(EditFormAction.ChangeTitle("No debe guardarse"))
        assertTrue(repository.savedDrafts.isEmpty())

        repository.loadFailure = IOException("cannot read")
        editor.retryLoad()
        runCurrent()
        assertEquals(DraftLoadError.FAILED, editor.uiState.value.loadError)
        repository.loadFailure = null
        repository.saveDraft(storedDraft().copy(id = "missing"))
        editor.retryLoad()
        runCurrent()
        assertNull(editor.uiState.value.loadError)
        assertEquals("Guardado", editor.uiState.value.title)
    }

    @Test fun anOldLoadCannotReplaceANewerDraftThatTheUserStarted() = runTest {
        val repository = FakeFormRepository(listOf(storedDraft())).apply { loadDelayMillis = 100 }
        val editor = editor(repository)
        editor.openDraft("stored")
        runCurrent()
        editor.startDraft("Nuevo", null)
        val newId = editor.uiState.value.draftId
        advanceUntilIdle()

        assertEquals(newId, editor.uiState.value.draftId)
        assertEquals("Nuevo", editor.uiState.value.title)
        assertFalse(editor.uiState.value.isLoading)
    }

    @Test fun androidRestorationPreservesTheIdAndRetriesTheLastContent() = runTest {
        val repository = FakeFormRepository()
        val saved = SavedStateHandle()
        val original = editor(repository, saved)
        original.startDraft("Recuperado", null)
        val id = original.uiState.value.draftId!!
        original.onAction(EditFormAction.AddField(FormFieldType.EMAIL, "Correo"))
        val values = saved.keys().associateWith { saved.get<Any?>(it) }
        val restored = editor(repository, SavedStateHandle(values))
        runCurrent()

        assertEquals(id, restored.uiState.value.draftId)
        assertEquals(original.uiState.value.fields, restored.uiState.value.fields)
        assertEquals(restored.uiState.value.fields, repository.getDraft(id)!!.fields)
    }

    @Test fun restoringWhileLoadingRetriesTheReadInsteadOfSavingAnEmptyDraft() = runTest {
        val repository = FakeFormRepository(listOf(storedDraft()))
        val saved = SavedStateHandle()
        val original = editor(repository, saved)
        original.openDraft("stored")
        val values = saved.keys().associateWith { saved.get<Any?>(it) }
        val restored = editor(repository, SavedStateHandle(values))
        runCurrent()

        assertEquals("Guardado", restored.uiState.value.title)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    private fun editor(repository: FakeFormRepository, saved: SavedStateHandle = SavedStateHandle()) =
        EditFormViewModel(FakeTemplateRepository(), saved, repository, now = { time }).also {
            viewModels.put("editor-${viewModelNumber++}", it)
        }

    private fun storedDraft() = FormDraft(
        "stored", "Guardado", listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre")), 20, 40
    )
}
