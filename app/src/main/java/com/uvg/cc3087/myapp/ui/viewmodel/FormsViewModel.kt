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
    repository: FormRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val savedFilter = savedStateHandle.getStateFlow(
        SELECTED_FILTER_KEY,
        FormFilter.ALL.name
    )

    private val reloadRequests = MutableStateFlow(0)
    private val sourceState = reloadRequests.flatMapLatest {
        repository.observeForms()
            .map { forms -> FormsUiState(forms = forms, status = FormsLoadStatus.CONTENT) }
            .onStart { emit(FormsUiState(status = FormsLoadStatus.LOADING)) }
            .catch { emit(FormsUiState(status = FormsLoadStatus.ERROR)) }
    }

    // si cambia la lista o el filtro, calculamos un solo estado para la pantalla
    val uiState: StateFlow<FormsUiState> = combine(
        sourceState,
        savedFilter
    ) { source, filterName ->
        val forms = source.forms
        val selectedFilter = restoreFilter(filterName)
        val visibleForms = when (selectedFilter) {
            FormFilter.ALL -> forms
            FormFilter.ACTIVE -> forms.filter { it.status == FormStatus.ACTIVE }
            FormFilter.DRAFT -> forms.filter { it.status == FormStatus.DRAFT }
        }

        // una lista con datos también puede quedar vacía al aplicar el filtro
        val status = when (source.status) {
            FormsLoadStatus.LOADING, FormsLoadStatus.ERROR -> source.status
            FormsLoadStatus.EMPTY, FormsLoadStatus.CONTENT ->
                if (visibleForms.isEmpty()) FormsLoadStatus.EMPTY else FormsLoadStatus.CONTENT
        }
        source.copy(forms = visibleForms, selectedFilter = selectedFilter, status = status)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FormsUiState(selectedFilter = restoreFilter(savedFilter.value))
    )

    fun retryLoading() {
        reloadRequests.update { it + 1 }
    }

    fun selectFilter(filter: FormFilter) {
        // guardamos el nombre estable para recuperar la selección al recrear la app
        savedStateHandle[SELECTED_FILTER_KEY] = filter.name
    }

    companion object {
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
