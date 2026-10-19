package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.ui.state.DraftLoadError
import com.uvg.cc3087.myapp.ui.state.SaveShareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SaveShareViewModel(
    private val repository: FormRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SaveShareUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null
    private var revision = 0L

    init {
        savedStateHandle.get<String>(DRAFT_ID_KEY)?.let(::openDraft)
    }

    fun openDraft(id: String) {
        savedStateHandle[DRAFT_ID_KEY] = id
        revision++
        val currentRevision = revision
        loadJob?.cancel()
        mutableUiState.value = SaveShareUiState(isLoading = true)
        loadJob = viewModelScope.launch {
            try {
                val draft = repository.getDraft(id)
                if (revision == currentRevision) {
                    // confirmamos lo que está en room, no una copia pendiente del editor
                    mutableUiState.value = SaveShareUiState(
                        draft = draft,
                        loadError = if (draft == null) DraftLoadError.NOT_FOUND else null
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision == currentRevision) {
                    mutableUiState.value = SaveShareUiState(loadError = DraftLoadError.FAILED)
                }
            }
        }
    }

    fun retryLoading() {
        savedStateHandle.get<String>(DRAFT_ID_KEY)?.let(::openDraft)
    }

    companion object {
        private const val DRAFT_ID_KEY = "save_share_draft_id"

        fun factory(repository: FormRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SaveShareViewModel(repository, createSavedStateHandle()) }
        }
    }
}
