package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.ui.state.FormFilter
import com.uvg.cc3087.myapp.ui.state.FormsUiState
import com.uvg.cc3087.myapp.ui.state.FormsLoadStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.ui.state.FormDeletionState
import com.uvg.cc3087.myapp.domain.search.FormSearch
import com.uvg.cc3087.myapp.ui.state.FormsEmptyReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class FormsViewModel(
    private val repository: FormRepository,
    private val savedStateHandle: SavedStateHandle,
    private val searchDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val savedFilter = savedStateHandle.getStateFlow(
        SELECTED_FILTER_KEY,
        FormFilter.ALL.name
    )

    private val savedQuery = savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, "")
    private val searchExpanded = savedStateHandle.getStateFlow(SEARCH_EXPANDED_KEY, false)

    private val deletion = MutableStateFlow(FormDeletionState())

    private val reloadRequests = MutableStateFlow(0)
    private val sourceState = reloadRequests.flatMapLatest {
        repository.observeForms()
            .map { forms -> FormsUiState(forms = forms, status = FormsLoadStatus.CONTENT) }
            .onStart { emit(FormsUiState(status = FormsLoadStatus.LOADING)) }
            .catch { emit(FormsUiState(status = FormsLoadStatus.ERROR)) }
    }

    private data class SearchInput(val source: FormsUiState, val filter: FormFilter, val query: String)

    private val results = combine(sourceState, savedFilter, savedQuery) { source, filter, query ->
        SearchInput(source, restoreFilter(filter), query)
    }.mapLatest { (source, filter, query) ->
        withContext(searchDispatcher) {
            val filtered = when (filter) {
                FormFilter.ALL -> source.forms
                FormFilter.ACTIVE -> source.forms.filter { it.status == FormStatus.ACTIVE }
                FormFilter.DRAFT -> source.forms.filter { it.status == FormStatus.DRAFT }
            }
            val visible = FormSearch.find(filtered, query)
            source.copy(
                forms = visible,
                selectedFilter = filter,
                searchQuery = query,
                status = when (source.status) {
                    FormsLoadStatus.LOADING, FormsLoadStatus.ERROR -> source.status
                    else -> if (visible.isEmpty()) FormsLoadStatus.EMPTY else FormsLoadStatus.CONTENT
                },
                emptyReason = when {
                    source.forms.isEmpty() -> FormsEmptyReason.SOURCE
                    filtered.isEmpty() -> FormsEmptyReason.FILTER
                    else -> FormsEmptyReason.SEARCH
                }
            )
        }
    }

    // Echo input immediately while cancellable matching runs on the worker dispatcher.
    // Never display results belonging to an older query.
    val uiState: StateFlow<FormsUiState> = combine(
        results, savedQuery, searchExpanded, deletion
    ) { result, query, expanded, deletionState ->
        result.copy(
            searchQuery = query, searchExpanded = expanded, deletion = deletionState,
            forms = if (result.searchQuery == query) result.forms else emptyList(),
            status = if (result.searchQuery == query) result.status else FormsLoadStatus.LOADING
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FormsUiState(
            selectedFilter = restoreFilter(savedFilter.value),
            searchQuery = savedQuery.value, searchExpanded = searchExpanded.value
        )
    )

    fun openSearch() { savedStateHandle[SEARCH_EXPANDED_KEY] = true }
    fun updateSearch(query: String) { savedStateHandle[SEARCH_QUERY_KEY] = query }
    fun clearSearch() { updateSearch("") }
    fun closeSearch() {
        clearSearch()
        savedStateHandle[SEARCH_EXPANDED_KEY] = false
    }
    fun resetSearchAndFilter() {
        clearSearch()
        selectFilter(FormFilter.ALL)
    }

    fun requestDeletion(form: FormSummary) {
        if (!deletion.value.inProgress) deletion.value = FormDeletionState(target = form)
    }

    fun dismissDeletion() {
        if (!deletion.value.inProgress) deletion.value = FormDeletionState()
    }

    fun consumeDeletionCompleted() {
        deletion.update { it.copy(completed = false) }
    }

    fun confirmDeletion() {
        val target = deletion.value.target ?: return
        if (deletion.value.inProgress) return
        deletion.value = FormDeletionState(target = target, inProgress = true)
        viewModelScope.launch {
            try {
                repository.deleteDraft(target.id)
                deletion.value = FormDeletionState(completed = true)
            } catch (cancelled: CancellationException) {
                deletion.value = FormDeletionState(target = target)
                throw cancelled
            } catch (_: Exception) {
                deletion.value = FormDeletionState(target = target, failed = true)
            }
        }
    }

    fun retryLoading() {
        reloadRequests.update { it + 1 }
    }

    fun selectFilter(filter: FormFilter) {
        // guardamos el nombre estable para recuperar la selección al recrear la app
        savedStateHandle[SELECTED_FILTER_KEY] = filter.name
    }

    companion object {
        private const val SEARCH_QUERY_KEY = "forms_search_query"
        private const val SEARCH_EXPANDED_KEY = "forms_search_expanded"
        private const val SELECTED_FILTER_KEY = "selected_forms_filter"

        private fun restoreFilter(name: String): FormFilter =
            FormFilter.entries.firstOrNull { it.name == name } ?: FormFilter.ALL

        fun factory(repository: FormRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FormsViewModel(repository, createSavedStateHandle())
            }
        }
    }
}
