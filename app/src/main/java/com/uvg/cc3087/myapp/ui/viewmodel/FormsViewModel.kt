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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FormsViewModel(
    repository: FormRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val savedFilter = savedStateHandle.getStateFlow(
        SELECTED_FILTER_KEY,
        FormFilter.ALL.name
    )

    // si cambia la lista o el filtro, calculamos un solo estado para la pantalla
    val uiState: StateFlow<FormsUiState> = combine(
        repository.observeForms(),
        savedFilter
    ) { forms, filterName ->
        val selectedFilter = restoreFilter(filterName)
        val visibleForms = when (selectedFilter) {
            FormFilter.ALL -> forms
            FormFilter.ACTIVE -> forms.filter { it.status == FormStatus.ACTIVE }
            FormFilter.DRAFT -> forms.filter { it.status == FormStatus.DRAFT }
        }

        FormsUiState(forms = visibleForms, selectedFilter = selectedFilter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FormsUiState(selectedFilter = restoreFilter(savedFilter.value))
    )

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
