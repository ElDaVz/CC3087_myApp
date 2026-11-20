package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.FormListPreferences
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.InMemoryFormListPreferences
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.FormFilter
import com.uvg.cc3087.myapp.ui.state.FormSortOrder
import com.uvg.cc3087.myapp.ui.state.FormsLoadStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException
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
        val viewModel = model(FakeFormRepository(forms), SavedStateHandle())
        observe(viewModel)

        assertEquals(FormFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals(forms, viewModel.uiState.value.forms)
    }

    @Test
    fun activeFilterShowsOnlyActiveForms() = runTest {
        val viewModel = model(FakeFormRepository(forms), SavedStateHandle())
        observe(viewModel)

        viewModel.selectFilter(FormFilter.ACTIVE)
        runCurrent()

        assertEquals(FormFilter.ACTIVE, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(forms[0], forms[2]), viewModel.uiState.value.forms)
    }

    @Test
    fun draftFilterShowsOnlyDraftForms() = runTest {
        val viewModel = model(FakeFormRepository(forms), SavedStateHandle())
        observe(viewModel)

        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()

        assertEquals(FormFilter.DRAFT, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(forms[1]), viewModel.uiState.value.forms)
    }

    @Test
    fun switchingBackToAllDoesNotLoseForms() = runTest {
        val repository = FakeFormRepository(forms)
        val viewModel = model(repository, SavedStateHandle())
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
        val viewModel = model(repository, SavedStateHandle())
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
        val viewModel = model(
            FakeFormRepository(listOf(forms[0])),
            SavedStateHandle()
        )
        observe(viewModel)

        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()

        assertEquals(FormFilter.DRAFT, viewModel.uiState.value.selectedFilter)
        assertTrue(viewModel.uiState.value.forms.isEmpty())
        assertEquals(FormsLoadStatus.EMPTY, viewModel.uiState.value.status)
    }

    @Test
    fun switchingAwayFromAnEmptyFilterRestoresContent() = runTest {
        val viewModel = model(FakeFormRepository(listOf(forms[0])), SavedStateHandle())
        observe(viewModel)

        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()
        assertEquals(FormsLoadStatus.EMPTY, viewModel.uiState.value.status)

        viewModel.selectFilter(FormFilter.ALL)
        runCurrent()
        assertEquals(FormsLoadStatus.CONTENT, viewModel.uiState.value.status)
        assertEquals(listOf(forms[0]), viewModel.uiState.value.forms)
    }

    @Test
    fun removingTheLastMatchingFormEmitsEmptyWithoutReloading() = runTest {
        val repository = FakeFormRepository(forms)
        val viewModel = model(repository, SavedStateHandle())
        observe(viewModel)
        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()

        repository.forms.value = listOf(forms[0])
        runCurrent()
        assertEquals(FormsLoadStatus.EMPTY, viewModel.uiState.value.status)

        repository.forms.value = forms
        runCurrent()
        assertEquals(FormsLoadStatus.CONTENT, viewModel.uiState.value.status)
        assertEquals(listOf(forms[1]), viewModel.uiState.value.forms)
    }

    @Test
    fun recreatingViewModelRestoresTheSavedFilter() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeFormRepository(forms)
        val original = model(repository, savedStateHandle)
        observe(original)
        original.selectFilter(FormFilter.DRAFT)
        runCurrent()

        // simulamos los valores restaurados, sin reutilizar la instancia anterior
        val savedValues = savedStateHandle.keys().associateWith { key ->
            savedStateHandle.get<Any?>(key)
        }
        val restored = model(repository, SavedStateHandle(savedValues))
        observe(restored)

        assertEquals(FormFilter.DRAFT, restored.uiState.value.selectedFilter)
        assertEquals(listOf(forms[1]), restored.uiState.value.forms)
    }

    @Test
    fun unknownSavedFilterFallsBackToAll() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf("selected_forms_filter" to "OLD_FILTER")
        )
        val viewModel = model(FakeFormRepository(forms), savedStateHandle)
        observe(viewModel)

        assertEquals(FormFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals(forms, viewModel.uiState.value.forms)
    }

    @Test
    fun aReadFailureCanBeRetriedWithoutLosingTheSelectedFilter() = runTest {
        var failRead = true
        val repository = object : FormRepository by FakeFormRepository(forms) {
            override fun observeForms(): Flow<List<FormSummary>> = flow {
                if (failRead) throw IOException("read failed")
                emit(forms)
            }
        }
        val viewModel = model(repository, SavedStateHandle())
        observe(viewModel)
        viewModel.selectFilter(FormFilter.DRAFT)
        runCurrent()
        assertEquals(FormsLoadStatus.ERROR, viewModel.uiState.value.status)
        failRead = false
        viewModel.retryLoading()
        runCurrent()

        assertEquals(FormsLoadStatus.CONTENT, viewModel.uiState.value.status)
        assertEquals(FormFilter.DRAFT, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(forms[1]), viewModel.uiState.value.forms)
    }

    @Test
    fun listExposesLoadingEmptyContentAndErrorStates() = runTest {
        val emptyModel = model(FakeFormRepository(emptyList()), SavedStateHandle())
        assertEquals(FormsLoadStatus.LOADING, emptyModel.uiState.value.status)
        observe(emptyModel)
        assertEquals(FormsLoadStatus.EMPTY, emptyModel.uiState.value.status)

        val contentModel = model(FakeFormRepository(forms), SavedStateHandle())
        observe(contentModel)
        assertEquals(FormsLoadStatus.CONTENT, contentModel.uiState.value.status)

        val broken = object : FormRepository by FakeFormRepository(forms) {
            override fun observeForms(): Flow<List<FormSummary>> = flow { throw IOException("read failed") }
        }
        val errorModel = model(broken, SavedStateHandle())
        observe(errorModel)
        assertEquals(FormsLoadStatus.ERROR, errorModel.uiState.value.status)
    }

    @Test fun deletionRequiresConfirmationAndCanBeCancelled() = runTest {
        val repository = FakeFormRepository(forms)
        val model = model(repository, SavedStateHandle())
        observe(model)
        model.requestDeletion(forms[0])
        runCurrent()
        assertEquals(forms[0], model.uiState.value.deletion.target)
        assertEquals(forms, repository.forms.value)
        model.dismissDeletion()
        model.confirmDeletion()
        runCurrent()
        assertEquals(forms, repository.forms.value)
        assertEquals(null, model.uiState.value.deletion.target)
    }

    @Test fun confirmedDeletionUpdatesTheFilteredListAndCannotRunTwice() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val backing = FakeFormRepository(forms)
        var calls = 0
        val repository = object : FormRepository by backing {
            override suspend fun deleteDraft(id: String) {
                calls++
                gate.await()
                backing.deleteDraft(id)
            }
        }
        val model = model(repository, SavedStateHandle())
        observe(model)
        model.selectFilter(FormFilter.DRAFT)
        model.requestDeletion(forms[1])
        model.confirmDeletion()
        model.confirmDeletion()
        model.dismissDeletion()
        runCurrent()
        assertEquals(1, calls)
        assertTrue(model.uiState.value.deletion.inProgress)
        gate.complete(Unit)
        runCurrent()
        assertEquals(FormsLoadStatus.EMPTY, model.uiState.value.status)
        assertEquals(listOf(forms[0], forms[2]), backing.forms.value)
        assertTrue(model.uiState.value.deletion.completed)
    }

    @Test fun failedDeletionKeepsTheFormAndSupportsRetry() = runTest {
        val backing = FakeFormRepository(forms)
        var fail = true
        val repository = object : FormRepository by backing {
            override suspend fun deleteDraft(id: String) {
                if (fail) throw IOException("write failed")
                backing.deleteDraft(id)
            }
        }
        val model = model(repository, SavedStateHandle())
        observe(model)
        model.requestDeletion(forms[0])
        model.confirmDeletion()
        runCurrent()
        assertTrue(model.uiState.value.deletion.failed)
        assertEquals(forms, backing.forms.value)
        fail = false
        model.confirmDeletion()
        runCurrent()
        assertTrue(model.uiState.value.deletion.completed)
        assertEquals(forms.drop(1), model.uiState.value.forms)
    }

    @Test fun searchCombinesWithFilterAndTracksRepositoryUpdates() = runTest {
        val repository = FakeFormRepository(forms)
        val vm = model(repository, SavedStateHandle())
        observe(vm)
        vm.openSearch()
        vm.updateSearch("middle")
        vm.selectFilter(FormFilter.ACTIVE)
        runCurrent()
        assertEquals(FormsLoadStatus.EMPTY, vm.uiState.value.status)
        assertEquals(com.uvg.cc3087.myapp.ui.state.FormsEmptyReason.SEARCH, vm.uiState.value.emptyReason)
        vm.selectFilter(FormFilter.DRAFT)
        runCurrent()
        assertEquals(listOf(forms[1]), vm.uiState.value.forms)
        repository.forms.value = listOf(forms[1].copy(title = "Changed"))
        runCurrent()
        assertTrue(vm.uiState.value.forms.isEmpty())
        vm.clearSearch()
        runCurrent()
        assertEquals("Changed", vm.uiState.value.forms.single().title)
        assertTrue(vm.uiState.value.searchExpanded)
        assertEquals(FormFilter.DRAFT, vm.uiState.value.selectedFilter)
        vm.closeSearch()
        runCurrent()
        assertEquals(false, vm.uiState.value.searchExpanded)
        assertEquals(FormFilter.DRAFT, vm.uiState.value.selectedFilter)
    }

    @Test fun restoredSearchIsScopedToItsOwnSavedStateAndResetKeepsFieldOpen() = runTest {
        val handle = SavedStateHandle()
        val repository = FakeFormRepository(forms)
        val original = model(repository, handle)
        observe(original)
        original.openSearch()
        original.updateSearch("middle")
        original.selectFilter(FormFilter.DRAFT)
        runCurrent()
        val restored = model(repository, SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        observe(restored)
        assertEquals("middle", restored.uiState.value.searchQuery)
        assertTrue(restored.uiState.value.searchExpanded)
        assertEquals(listOf(forms[1]), restored.uiState.value.forms)
        val otherSession = model(repository, SavedStateHandle())
        observe(otherSession)
        assertEquals("", otherSession.uiState.value.searchQuery)
        assertEquals(false, otherSession.uiState.value.searchExpanded)
        restored.resetSearchAndFilter()
        runCurrent()
        assertEquals(forms, restored.uiState.value.forms)
        assertEquals(FormFilter.ALL, restored.uiState.value.selectedFilter)
        assertTrue(restored.uiState.value.searchExpanded)
        assertEquals("middle", original.uiState.value.searchQuery)
    }

    @Test fun emptyReasonsDistinguishSourceFilterAndSearch() = runTest {
        val repository = FakeFormRepository(emptyList())
        val vm = model(repository, SavedStateHandle())
        observe(vm)
        vm.updateSearch("missing")
        runCurrent()
        assertEquals(com.uvg.cc3087.myapp.ui.state.FormsEmptyReason.SOURCE, vm.uiState.value.emptyReason)
        repository.forms.value = listOf(forms[0])
        vm.selectFilter(FormFilter.DRAFT)
        runCurrent()
        assertEquals(com.uvg.cc3087.myapp.ui.state.FormsEmptyReason.FILTER, vm.uiState.value.emptyReason)
        vm.selectFilter(FormFilter.ALL)
        runCurrent()
        assertEquals(com.uvg.cc3087.myapp.ui.state.FormsEmptyReason.SEARCH, vm.uiState.value.emptyReason)
    }

    @Test fun sortUsesStoredCountsAndNumericModificationTime() = runTest {
        val preferences = InMemoryFormListPreferences()
        preferences.set("owner-a", FormSortOrder.FEWEST_RESPONSES)
        val list = listOf(
            form("many", FormStatus.DRAFT).copy(responseCount = 9, updatedAt = 30),
            form("few-new", FormStatus.DRAFT).copy(responseCount = 1, updatedAt = 20),
            form("few-old", FormStatus.DRAFT).copy(responseCount = 1, updatedAt = 10)
        )
        val vm = FormsViewModel(
            FakeFormRepository(list), SavedStateHandle(), mainDispatcherRule.dispatcher,
            preferences, "owner-a"
        )
        observe(vm)

        assertEquals(listOf(list[1], list[2], list[0]), vm.uiState.value.forms)

        vm.selectSortOrder(FormSortOrder.MOST_RESPONSES)
        runCurrent()
        assertEquals(listOf(list[0], list[1], list[2]), vm.uiState.value.forms)
    }

    @Test fun sortPreferenceIsPartitionedByOwner() = runTest {
        val preferences = InMemoryFormListPreferences()
        preferences.set("owner-a", FormSortOrder.OLDEST_FIRST)
        val vm = model(FakeFormRepository(forms), SavedStateHandle(), preferences, "owner-b")
        observe(vm)
        assertEquals(FormSortOrder.RECENT_FIRST, vm.uiState.value.selectedSort)
    }

    @Test fun searchRelevanceComesBeforeTheChosenOrder() = runTest {
        val preferences = InMemoryFormListPreferences()
        preferences.set("guest", FormSortOrder.MOST_RESPONSES)
        val exact = form("exact", FormStatus.DRAFT).copy(title = "Pedido", responseCount = 0, updatedAt = 1)
        val low = form("low", FormStatus.DRAFT).copy(title = "Pedido A", responseCount = 1, updatedAt = 1)
        val high = form("high", FormStatus.DRAFT).copy(title = "Pedido B", responseCount = 5, updatedAt = 2)
        val vm = FormsViewModel(
            FakeFormRepository(listOf(low, exact, high)), SavedStateHandle(),
            mainDispatcherRule.dispatcher, preferences, "guest"
        )
        observe(vm)
        vm.updateSearch("pedido")
        runCurrent()

        assertEquals(listOf(exact, high, low), vm.uiState.value.forms)
    }

    @Test fun latestQueryWinsWhenWorkerHasNotRunAndRetryPreservesSearch() = runTest {
        val worker = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        var fail = true
        val repository = object : FormRepository by FakeFormRepository(forms) {
            override fun observeForms(): Flow<List<FormSummary>> = flow {
                if (fail) throw IOException("read failed")
                emit(forms)
            }
        }
        val vm = FormsViewModel(repository, SavedStateHandle(), worker)
        observe(vm)
        vm.openSearch()
        vm.updateSearch("first")
        vm.updateSearch("last")
        runCurrent()
        assertEquals(FormsLoadStatus.ERROR, vm.uiState.value.status)
        fail = false
        vm.retryLoading()
        runCurrent()
        assertEquals("last", vm.uiState.value.searchQuery)
        assertEquals(listOf(forms[2]), vm.uiState.value.forms)
    }

    private fun model(repository: FormRepository, handle: SavedStateHandle) =
        FormsViewModel(repository, handle, mainDispatcherRule.dispatcher)

    private fun model(
        repository: FormRepository, handle: SavedStateHandle,
        preferences: FormListPreferences, owner: String
    ) = FormsViewModel(repository, handle, mainDispatcherRule.dispatcher, preferences, owner)

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
        override suspend fun getDraft(id: String): FormDraft? = null
        override suspend fun saveDraft(draft: FormDraft) = Unit
        override suspend fun deleteDraft(id: String) { forms.value = forms.value.filterNot { it.id == id } }
    }

    private var nextUpdatedAt = 1_000L

    private fun form(id: String, status: FormStatus) = FormSummary(
        id = id,
        title = id,
        responseCount = 0,
        updatedDate = "8 oct",
        status = status,
        imageUrl = "",
        updatedAt = nextUpdatedAt--
    )
}
