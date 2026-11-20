package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.InvitationMode
import com.uvg.cc3087.myapp.data.repository.PublicationError
import com.uvg.cc3087.myapp.data.repository.PublicationException
import com.uvg.cc3087.myapp.ui.state.DraftLoadError
import com.uvg.cc3087.myapp.ui.state.SaveShareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SaveShareViewModel(
    private val repository: FormRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SaveShareUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null
    private var revision = 0L
    private var summary: FormSummary? = null

    init {
        // el conteo y el estado viven en el repositorio; sin conexión conservamos el último dato local
        viewModelScope.launch {
            repository.observeForms().catch { /* mantiene el último resumen conocido */ }
                .collect { forms ->
                    val id = savedStateHandle.get<String>(DRAFT_ID_KEY) ?: return@collect
                    val found = forms.firstOrNull { it.id == id } ?: return@collect
                    summary = found
                    mutableUiState.value = mutableUiState.value.copy(
                        responseCount = found.responseCount, formStatus = found.status
                    )
                }
        }
        savedStateHandle.get<String>(DRAFT_ID_KEY)?.let(::openDraft)
    }

    private fun SaveShareUiState.withSummary(): SaveShareUiState = copy(
        responseCount = summary?.responseCount ?: 0,
        formStatus = summary?.status ?: FormStatus.DRAFT
    )

    fun openDraft(id: String) {
        if (savedStateHandle.get<String>(DRAFT_ID_KEY) != id) summary = null
        savedStateHandle[DRAFT_ID_KEY] = id
        revision++
        val currentRevision = revision
        loadJob?.cancel()
        mutableUiState.value = SaveShareUiState(isLoading = true).withSummary()
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
                    ).withSummary()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision == currentRevision) {
                    mutableUiState.value = SaveShareUiState(loadError = DraftLoadError.FAILED).withSummary()
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
                mutableUiState.value = mutableUiState.value.copy(
                    publication = published, publicLink = published.url, publishing = false,
                    formStatus = if (published.status == "PUBLISHED") FormStatus.ACTIVE else mutableUiState.value.formStatus
                )
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (revision != operationRevision) return@launch
                mutableUiState.value = mutableUiState.value.copy(publishing = false,
                    publishError = (error as? PublicationException)?.reason
                        ?: PublicationError.PUBLISH_FAILED)
            }
        }
    }
    fun selectInvitationMode(mode: InvitationMode) {
        mutableUiState.value = mutableUiState.value.copy(invitationMode = mode)
    }

    fun archive() {
        val state = mutableUiState.value
        if (state.publishing) return
        val id = state.draft?.id ?: return
        mutableUiState.value = state.copy(publishing = true, publishError = null)
        viewModelScope.launch {
            try { repository.archive(id); openDraft(id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableUiState.value = mutableUiState.value.copy(publishing = false, publishError = PublicationError.ARCHIVE_FAILED) }
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
