package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.RequestAccessDeniedException
import com.uvg.cc3087.myapp.data.repository.RequestBackendUnavailableException
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestListUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RequestListViewModel(private val repository: RequestRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RequestListUiState())
    val uiState = mutableUiState.asStateFlow()
    private var job: Job? = null
    private var revision = 0L

    init { refresh() }

    fun refresh() {
        val currentRevision = ++revision
        job?.cancel()
        mutableUiState.value = RequestListUiState()
        job = viewModelScope.launch {
            val result = try {
                repository.observeRequests().collect { source ->
                    val requests = source.distinctBy { it.key }.sortedByDescending { it.submittedAt }
                    val next = RequestListUiState(
                        if (requests.isEmpty()) RequestListLoadStatus.EMPTY else RequestListLoadStatus.CONTENT,
                        requests
                    )
                    if (revision == currentRevision) mutableUiState.value = next
                }
                null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RequestAccessDeniedException) {
                RequestListUiState(RequestListLoadStatus.FORBIDDEN)
            } catch (_: RequestBackendUnavailableException) {
                RequestListUiState(RequestListLoadStatus.UNAVAILABLE)
            } catch (_: Exception) {
                RequestListUiState(RequestListLoadStatus.ERROR)
            }
            if (revision == currentRevision && result != null) mutableUiState.value = result
        }
    }

    companion object {
        fun factory(repository: RequestRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RequestListViewModel(repository) }
        }
    }
}
