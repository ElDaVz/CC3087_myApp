package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.RequestAccessDeniedException
import com.uvg.cc3087.myapp.data.repository.RequestBackendUnavailableException
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.ui.state.RequestDetailLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestDetailUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RequestDetailViewModel(
    private val repository: RequestRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RequestDetailUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null
    private var readJob: Job? = null
    private var revision = 0L
    private var selection: Pair<String, String>? = null

    init {
        val formId = savedStateHandle.get<String>(FORM_ID)
        val responseId = savedStateHandle.get<String>(RESPONSE_ID)
        if (formId != null && responseId != null) openRequest(formId, responseId)
    }

    fun openRequest(formId: String, responseId: String, refresh: Boolean = false) {
        val next = formId to responseId
        if (selection == next && !refresh) return
        selection = next
        savedStateHandle[FORM_ID] = formId
        savedStateHandle[RESPONSE_ID] = responseId
        load(formId, responseId)
    }

    fun retryLoading() {
        selection?.let { (formId, responseId) -> load(formId, responseId) }
    }

    fun markRead() {
        val state = mutableUiState.value
        val detail = state.detail ?: return
        if (state.status != RequestDetailLoadStatus.CONTENT || state.readSaved || state.markingRead) return
        val stamp = revision
        mutableUiState.value = state.copy(markingRead = true, readSaveFailed = false)
        readJob = viewModelScope.launch {
            try {
                repository.markRead(detail.formId, detail.responseId)
                if (stamp == revision) mutableUiState.value = mutableUiState.value.copy(markingRead = false, readSaved = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (stamp == revision) mutableUiState.value = mutableUiState.value.copy(markingRead = false, readSaveFailed = true)
            }
        }
    }

    fun changeStatus(status: com.uvg.cc3087.myapp.data.model.RequestStatus) {
        val state = mutableUiState.value
        val detail = state.detail ?: return
        if (state.savingStatus || detail.requestStatus == null || detail.requestStatus == status) return
        val currentRevision = revision
        mutableUiState.value = state.copy(savingStatus = true, statusSaveFailed = false)
        viewModelScope.launch {
            try {
                val updated = repository.updateStatus(detail.formId, detail.responseId, status)
                check(updated.formId == detail.formId && updated.responseId == detail.responseId)
                if (revision == currentRevision) mutableUiState.value = mutableUiState.value.copy(detail = updated, savingStatus = false, statusSaveFailed = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision == currentRevision) mutableUiState.value = mutableUiState.value.copy(savingStatus = false, statusSaveFailed = true)
            }
        }
    }

    private fun load(formId: String, responseId: String) {
        val currentRevision = ++revision
        loadJob?.cancel()
        readJob?.cancel()
        // Clear previous answers immediately so another selection never shows stale client data.
        mutableUiState.value = RequestDetailUiState(RequestDetailLoadStatus.LOADING)
        if (formId.isBlank() || responseId.isBlank()) {
            mutableUiState.value = RequestDetailUiState(RequestDetailLoadStatus.NOT_FOUND)
            return
        }
        loadJob = viewModelScope.launch {
            val result = try {
                val detail = repository.getDetail(formId, responseId)
                check(detail == null || (detail.formId == formId && detail.responseId == responseId))
                if (detail == null) RequestDetailUiState(RequestDetailLoadStatus.NOT_FOUND)
                else RequestDetailUiState(RequestDetailLoadStatus.CONTENT, detail)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RequestAccessDeniedException) {
                RequestDetailUiState(RequestDetailLoadStatus.FORBIDDEN)
            } catch (_: RequestBackendUnavailableException) {
                RequestDetailUiState(RequestDetailLoadStatus.UNAVAILABLE)
            } catch (_: Exception) {
                RequestDetailUiState(RequestDetailLoadStatus.ERROR)
            }
            if (revision == currentRevision) mutableUiState.value = result
        }
    }

    companion object {
        private const val FORM_ID = "request_detail_form_id"
        private const val RESPONSE_ID = "request_detail_response_id"

        fun factory(repository: RequestRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RequestDetailViewModel(repository, createSavedStateHandle()) }
        }
    }
}
