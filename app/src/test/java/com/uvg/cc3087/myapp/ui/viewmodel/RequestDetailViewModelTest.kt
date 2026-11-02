package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.repository.RequestAccessDeniedException
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.RequestDetailLoadStatus
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestDetailViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    private var nextModel = 0
    private val detail = RequestDetail("form", "response", "Pedidos", null, emptyList())

    @After fun cleanup() { store.clear() }

    @Test fun failedStatusWriteKeepsThePersistedStateAndCanBeRetried() = runTest {
        val pending = detail.copy(requestStatus = com.uvg.cc3087.myapp.data.model.RequestStatus.PENDING)
        var fail = true
        val model = model(object : RequestRepository {
            override suspend fun getDetail(formId: String, responseId: String) = pending
            override suspend fun updateStatus(formId: String, responseId: String, status: com.uvg.cc3087.myapp.data.model.RequestStatus): RequestDetail {
                if (fail) throw IOException()
                return pending.copy(requestStatus = status)
            }
        })
        model.openRequest("form", "response")
        runCurrent()
        model.changeStatus(com.uvg.cc3087.myapp.data.model.RequestStatus.COMPLETED)
        runCurrent()
        assertEquals(pending, model.uiState.value.detail)
        assertTrue(model.uiState.value.statusSaveFailed)
        fail = false
        model.changeStatus(com.uvg.cc3087.myapp.data.model.RequestStatus.COMPLETED)
        runCurrent()
        assertFalse(model.uiState.value.statusSaveFailed)
        assertEquals(com.uvg.cc3087.myapp.data.model.RequestStatus.COMPLETED, model.uiState.value.detail?.requestStatus)
    }

    @Test fun readsTheSelectedFormAndResponseWithoutInventingAStatus() = runTest {
        var received: Pair<String, String>? = null
        val model = model(RequestRepository { form, response -> received = form to response; detail })
        model.openRequest("form", "response")
        assertEquals(RequestDetailLoadStatus.LOADING, model.uiState.value.status)
        runCurrent()
        assertEquals("form" to "response", received)
        assertEquals(detail, model.uiState.value.detail)
        assertNull(model.uiState.value.detail?.statusLabel)
    }

    @Test fun missingAndForbiddenResponsesNeverExposePreviousData() = runTest {
        val model = model(RequestRepository { _, response ->
            when (response) {
                "response" -> detail
                "forbidden" -> throw RequestAccessDeniedException()
                else -> null
            }
        })
        model.openRequest("form", "response")
        runCurrent()
        model.openRequest("form", "missing")
        assertNull(model.uiState.value.detail)
        runCurrent()
        assertEquals(RequestDetailLoadStatus.NOT_FOUND, model.uiState.value.status)
        model.openRequest("form", "forbidden")
        runCurrent()
        assertEquals(RequestDetailLoadStatus.FORBIDDEN, model.uiState.value.status)
        assertNull(model.uiState.value.detail)
    }

    @Test fun aConnectionFailureCanBeRetried() = runTest {
        var fail = true
        val model = model(RequestRepository { _, _ -> if (fail) throw IOException() else detail })
        model.openRequest("form", "response")
        runCurrent()
        assertEquals(RequestDetailLoadStatus.ERROR, model.uiState.value.status)
        fail = false
        model.retryLoading()
        runCurrent()
        assertEquals(detail, model.uiState.value.detail)
    }

    @Test fun restoresTheSelectionAfterRecreationAndReadsAgain() = runTest {
        val saved = SavedStateHandle()
        var reads = 0
        val repository = RequestRepository { _, _ -> reads++; detail }
        model(repository, saved).openRequest("form", "response")
        runCurrent()
        val values = saved.keys().associateWith { saved.get<Any?>(it) }
        val restored = model(repository, SavedStateHandle(values))
        runCurrent()
        assertEquals(detail, restored.uiState.value.detail)
        assertEquals(2, reads)
    }

    @Test fun anOlderNonCancellableReadCannotOverwriteAnotherSelection() = runTest {
        val gate = CompletableDeferred<Unit>()
        val newer = detail.copy(responseId = "newer")
        val model = model(RequestRepository { _, response ->
            if (response == "response") withContext(NonCancellable) { gate.await(); detail }
            else newer
        })
        model.openRequest("form", "response")
        runCurrent()
        model.openRequest("form", "newer")
        runCurrent()
        assertEquals(newer, model.uiState.value.detail)
        gate.complete(Unit)
        runCurrent()
        assertEquals(newer, model.uiState.value.detail)
    }

    @Test fun rejectsARepositoryResultFromADifferentForm() = runTest {
        val model = model(RequestRepository { _, _ -> detail.copy(formId = "someone-else") })
        model.openRequest("form", "response")
        runCurrent()
        assertEquals(RequestDetailLoadStatus.ERROR, model.uiState.value.status)
        assertNull(model.uiState.value.detail)
    }

    @Test fun reopeningTheSameSelectionDoesNotReloadButRetryDoes() = runTest {
        var reads = 0
        val model = model(RequestRepository { _, _ -> reads++; detail })
        model.openRequest("form", "response")
        runCurrent()
        model.openRequest("form", "response")
        runCurrent()
        assertEquals(1, reads)
        model.retryLoading()
        runCurrent()
        assertEquals(2, reads)
    }

    @Test fun blankIdsDoNotQueryTheRepository() = runTest {
        val model = model(RequestRepository { _, _ -> error("No request should be made") })
        model.openRequest("", "response")
        runCurrent()
        assertEquals(RequestDetailLoadStatus.NOT_FOUND, model.uiState.value.status)
    }

    @Test fun reopeningFromTheListCanRefreshTheSameResponse() = runTest {
        var latest = detail
        val model = model(RequestRepository { _, _ -> latest })
        model.openRequest("form", "response")
        runCurrent()
        latest = detail.copy(statusLabel = "Actualizado")
        model.openRequest("form", "response", refresh = true)
        runCurrent()
        assertEquals(latest, model.uiState.value.detail)
    }

    @Test fun unavailableBackendHasADistinctState() = runTest {
        val model = model(com.uvg.cc3087.myapp.data.repository.UnavailableRequestRepository)
        model.openRequest("form", "response")
        runCurrent()
        assertEquals(RequestDetailLoadStatus.UNAVAILABLE, model.uiState.value.status)
    }

    private fun model(repository: RequestRepository, saved: SavedStateHandle = SavedStateHandle()) =
        RequestDetailViewModel(repository, saved).also { store.put("detail-${nextModel++}", it) }
}
