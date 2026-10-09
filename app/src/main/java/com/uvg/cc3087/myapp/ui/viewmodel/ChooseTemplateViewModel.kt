package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.ui.state.ChooseTemplateUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ChooseTemplateViewModel(repository: TemplateRepository) : ViewModel() {
    // la pantalla recibe la lista desde aquí, no construye sus propios datos
    val uiState: StateFlow<ChooseTemplateUiState> = repository.observeTemplates()
        .map { templates -> ChooseTemplateUiState(templates = templates) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChooseTemplateUiState()
        )

    companion object {
        fun factory(repository: TemplateRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChooseTemplateViewModel(repository) }
        }
    }
}
