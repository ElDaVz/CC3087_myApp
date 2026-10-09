package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.FormFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val forms = listOf(
        form("active-first", FormStatus.ACTIVE),
        form("draft-middle", FormStatus.DRAFT),
        form("active-last", FormStatus.ACTIVE)
    )

    @Test
    fun allFilterKeepsRepositoryOrder() = runTest {
        val viewModel = FormsViewModel(FakeFormRepository(forms), SavedStateHandle())
        observe(viewModel)

        assertEquals(FormFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals(forms, viewModel.uiState.value.forms)
    }

    @Test
    fun activeFilterShowsOnlyActiveForms() = runTest {
        val viewModel = FormsViewModel(FakeFormRepository(forms), SavedStateHandle())
        observe(viewModel)

        viewModel.selectFilter(FormFilter.ACTIVE)
        runCurrent()

        assertEquals(FormFilter.ACTIVE, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(forms[0], forms[2]), viewModel.uiState.value.forms)
    }

    @Test
    fun draftFilterShowsOnlyDraftForms() = runTest {
        val viewModel = FormsViewModel(FakeFormRepository(forms), SavedStateHandle())
        observe(viewModel)

        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()

        assertEquals(FormFilter.DRAFT, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(forms[1]), viewModel.uiState.value.forms)
    }

    @Test
    fun switchingBackToAllDoesNotLoseForms() = runTest {
        val repository = FakeFormRepository(forms)
        val viewModel = FormsViewModel(repository, SavedStateHandle())
        observe(viewModel)

        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()
        viewModel.selectFilter(FormFilter.ALL)
        runCurrent()

        assertEquals(forms, viewModel.uiState.value.forms)
        assertEquals(forms, repository.forms.value)
    }

    @Test
    fun repositoryUpdatesRespectTheSelectedFilter() = runTest {
        val repository = FakeFormRepository(forms)
        val viewModel = FormsViewModel(repository, SavedStateHandle())
        observe(viewModel)
        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()

        val newDraft = form("new-draft", FormStatus.DRAFT)
        repository.forms.value = listOf(newDraft, forms[0])
        runCurrent()

        assertEquals(FormFilter.DRAFT, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(newDraft), viewModel.uiState.value.forms)
    }

    @Test
    fun filterWithoutMatchesReturnsAnEmptyList() = runTest {
        val viewModel = FormsViewModel(
            FakeFormRepository(listOf(forms[0])),
            SavedStateHandle()
        )
        observe(viewModel)

        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()

        assertEquals(FormFilter.DRAFT, viewModel.uiState.value.selectedFilter)
        assertTrue(viewModel.uiState.value.forms.isEmpty())
    }

    @Test
    fun recreatingViewModelRestoresTheSavedFilter() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeFormRepository(forms)
        val original = FormsViewModel(repository, savedStateHandle)
        observe(original)
        original.selectFilter(FormFilter.DRAFT)
        runCurrent()

        // simulamos los valores restaurados, sin reutilizar la instancia anterior
        val savedValues = savedStateHandle.keys().associateWith { key ->
            savedStateHandle.get<Any?>(key)
        }
        val restored = FormsViewModel(repository, SavedStateHandle(savedValues))
        observe(restored)

        assertEquals(FormFilter.DRAFT, restored.uiState.value.selectedFilter)
        assertEquals(listOf(forms[1]), restored.uiState.value.forms)
    }

    @Test
    fun unknownSavedFilterFallsBackToAll() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf("selected_forms_filter" to "OLD_FILTER")
        )
        val viewModel = FormsViewModel(FakeFormRepository(forms), savedStateHandle)
        observe(viewModel)

        assertEquals(FormFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals(forms, viewModel.uiState.value.forms)
    }

    private fun TestScope.observe(viewModel: FormsViewModel) {
        // statein comienza a observar el repositorio cuando tiene una suscripción
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()
    }

    private class FakeFormRepository(initialForms: List<FormSummary>) : FormRepository {
        val forms = MutableStateFlow(initialForms)

        override fun observeForms(): Flow<List<FormSummary>> = forms
    }

    private fun form(id: String, status: FormStatus) = FormSummary(
        id = id,
        title = id,
        responseCount = 0,
        updatedDate = "8 oct",
        status = status,
        imageUrl = ""
    )
}
