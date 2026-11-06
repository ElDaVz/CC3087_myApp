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
                    val publication = draft?.let { repository.publication(id) }
                    mutableUiState.value = SaveShareUiState(
                        draft = draft, publication = publication,
                        publicLink = publication?.takeIf { it.status == "PUBLISHED" }?.url,
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

    fun publish() {
        val state = mutableUiState.value
        val id = state.draft?.id ?: return
        if (state.publishing) return
        val operationRevision = revision
        mutableUiState.value = state.copy(publishing = true, publishError = null)
        viewModelScope.launch {
            try {
                val published = repository.publish(id)
                if (revision != operationRevision) return@launch
                mutableUiState.value = mutableUiState.value.copy(publication = published, publicLink = published.url, publishing = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (revision != operationRevision) return@launch
                mutableUiState.value = mutableUiState.value.copy(publishing = false,
                    publishError = (error as? com.uvg.cc3087.myapp.data.repository.PublicationException)?.message
                        ?: "No se pudo confirmar la publicación. Comprueba la conexión y reintenta.")
            }
        }
    }
    fun archive() {
        val state = mutableUiState.value
        if (state.publishing) return
        val id = state.draft?.id ?: return
        mutableUiState.value = state.copy(publishing = true, publishError = null)
        viewModelScope.launch {
            try { repository.archive(id); openDraft(id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableUiState.value = mutableUiState.value.copy(publishing = false, publishError = "No se pudo retirar la publicación. Reintenta con conexión.") }
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
