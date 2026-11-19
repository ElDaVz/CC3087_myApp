package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.SavedStateHandle
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.repository.RequestAccessDeniedException
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.data.repository.UnavailableRequestRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import java.io.IOException
import java.time.Instant
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
class ResponseDashboardInboxTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    private var modelId = 0

    @After fun cleanup() { store.clear() }

    @Test fun sortsNewestFirstAndKeepsIdenticalResponseIdsFromDifferentForms() = runTest {
        val older = summary("form-a", 1)
        val newer = summary("form-b", 2)
        val unknownDate = summary("form-c", 3).copy(submittedAt = null)
        val model = model(repository { listOf(unknownDate, older, newer, older) })
        runCurrent()
        assertEquals(listOf(newer, older, unknownDate), model.uiState.value.dashboard.requests)
        assertEquals(RequestListLoadStatus.CONTENT, model.uiState.value.status)
    }

    @Test fun unconfiguredBackendIsNotPresentedAsAnEmptyInbox() = runTest {
        val empty = model(repository { emptyList() })
        val unavailable = model(UnavailableRequestRepository)
        runCurrent()
        assertEquals(RequestListLoadStatus.CONTENT, empty.uiState.value.status)
        assertTrue(empty.uiState.value.dashboard.sourceEmpty)
        assertEquals(RequestListLoadStatus.UNAVAILABLE, unavailable.uiState.value.status)
    }

    @Test fun retryCanRecoverFromAConnectionFailure() = runTest {
        var fail = true
        val expected = summary("form-a", 1)
        val model = model(repository { if (fail) throw IOException() else listOf(expected) })
        runCurrent()
        assertEquals(RequestListLoadStatus.ERROR, model.uiState.value.status)
        fail = false
        model.refresh()
        runCurrent()
        assertEquals(listOf(expected), model.uiState.value.dashboard.requests)
    }

    @Test fun lossOfAccessRemovesPreviouslyLoadedCustomerData() = runTest {
        var denied = false
        val model = model(repository {
            if (denied) throw RequestAccessDeniedException() else listOf(summary("form-a", 1))
        })
        runCurrent()
        denied = true
        model.refresh()
        runCurrent()
        assertEquals(RequestListLoadStatus.FORBIDDEN, model.uiState.value.status)
        assertTrue(model.uiState.value.dashboard.requests.isEmpty())
        assertTrue(model.uiState.value.dashboard.forms.isEmpty())
        assertFalse(model.uiState.value.dashboard.hasUnread)
        assertFalse(model.uiState.value.hasData)
    }

    @Test fun lateRefreshCannotReplaceNewerResults() = runTest {
        val gate = CompletableDeferred<Unit>()
        var reads = 0
        val current = summary("new-form", 2)
        val model = model(repository {
            if (++reads == 1) withContext(NonCancellable) { gate.await(); listOf(summary("old-form", 1)) }
            else listOf(current)
        })
        runCurrent()
        model.refresh()
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(current), model.uiState.value.dashboard.requests)
    }

    private fun summary(form: String, epoch: Long) =
        RequestSummary(form, "shared-id", "Pedido", Instant.ofEpochSecond(epoch))

    private fun repository(load: suspend () -> List<RequestSummary>) = object : RequestRepository {
        override suspend fun getDetail(formId: String, responseId: String): RequestDetail? = null
        override suspend fun getRequests() = load()
    }

    private fun model(repository: RequestRepository) =
        ResponseDashboardViewModel(repository, SavedStateHandle()).apply { selectPeriod(ResponsePeriod.ALL) }.also { store.put("list-${modelId++}", it) }
}
