package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class ResponseDashboardViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun clear() { store.clear() }
    private val now = Instant.parse("2026-11-12T18:00:00Z")
    private val row = RequestSummary("f", "r", "Form", now)
    private class Repository : RequestRepository {
        val rows = MutableStateFlow(emptyList<RequestSummary>())
        var refreshes = 0
        var fail = false
        override suspend fun getDetail(formId: String, responseId: String) = null
        override fun observeRequests(): Flow<List<RequestSummary>> = flow { if (fail) throw java.io.IOException(); emitAll(rows) }
        override fun requestRefresh() { refreshes++ }
    }
    private fun model(repo: RequestRepository, saved: SavedStateHandle = SavedStateHandle(), clock: Clock = Clock.fixed(now, ZoneOffset.UTC)) =
        ResponseDashboardViewModel(repo, saved, clock) { ZoneOffset.UTC }.also { store.put("dashboard", it) }

    @Test fun streamsCountsReadFlagsAndStatusChangesAndRestoresFilters() = runTest {
        val repo = Repository(); val saved = SavedStateHandle(); val model = model(repo, saved)
        repo.rows.value = listOf(row); runCurrent()
        assertTrue(model.uiState.value.dashboard.hasUnread)
        repo.rows.value = listOf(row.copy(isRead = true)); runCurrent()
        assertFalse(model.uiState.value.dashboard.hasUnread)
        model.selectForm("f"); model.selectPeriod(ResponsePeriod.ALL)
        assertEquals("f", saved.get<String>("dashboard_form"))
        assertEquals(ResponsePeriod.ALL, model(repo, saved).uiState.value.filter.period)
    }
    @Test fun refreshRetainsCachedDataOnFailureAndCanRecover() = runTest {
        val repo = Repository(); val model = model(repo)
        repo.rows.value = listOf(row); runCurrent()
        repo.fail = true; model.refresh(); runCurrent()
        assertEquals(1, repo.refreshes)
        assertEquals(RequestListLoadStatus.ERROR, model.uiState.value.status)
        assertEquals(listOf(row), model.uiState.value.dashboard.requests)
        repo.fail = false; model.refresh(); runCurrent()
        assertEquals(RequestListLoadStatus.CONTENT, model.uiState.value.status)
    }
    @Test fun dayChangeRecomputesRollingWindowWithoutNewRows() = runTest {
        var instant = now
        val clock = object : Clock() {
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId) = this
            override fun instant() = instant
        }
        val repo = Repository(); val model = model(repo, clock = clock)
        repo.rows.value = listOf(row.copy(submittedAt = Instant.parse("2026-10-14T18:00:00Z"))); runCurrent()
        assertEquals(1, model.uiState.value.dashboard.requests.size)
        instant = now.plusSeconds(86400); model.checkCalendar()
        assertTrue(model.uiState.value.dashboard.requests.isEmpty())
    }
    @Test fun disposedSessionDoesNotAcceptFurtherData() = runTest {
        val repo = Repository(); val model = model(repo)
        repo.rows.value = listOf(row); runCurrent(); store.clear()
        repo.rows.value = listOf(row.copy(formId = "other")); runCurrent()
        assertEquals("f", model.uiState.value.dashboard.requests.single().formId)
        assertEquals(0, repo.rows.subscriptionCount.value)
    }

    @Test fun filtersDoNotHideErrorsOrUnavailableSources() = runTest {
        val failures = listOf(
            java.io.IOException() to RequestListLoadStatus.ERROR,
            com.uvg.cc3087.myapp.data.repository.RequestBackendUnavailableException() to RequestListLoadStatus.UNAVAILABLE,
            com.uvg.cc3087.myapp.data.repository.RequestAccessDeniedException() to RequestListLoadStatus.FORBIDDEN
        )
        failures.forEach { (error, expected) ->
            val repository = object : RequestRepository {
                override suspend fun getDetail(formId: String, responseId: String) = null
                override fun observeRequests(): Flow<List<RequestSummary>> = flow { throw error }
            }
            val model = model(repository)
            runCurrent()
            model.selectForm("f")
            model.selectPeriod(ResponsePeriod.ALL)
            assertEquals(expected, model.uiState.value.status)
            assertFalse(model.uiState.value.hasData)
            assertFalse(model.uiState.value.isRefreshing)
        }
    }

    @Test fun calendarAndTimeZoneChangesKeepAccessDeniedAndCustomerDataCleared() = runTest {
        var instant = now
        var zone = ZoneId.of("America/Guatemala")
        var denied = false
        val clock = object : Clock() {
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId) = this
            override fun instant() = instant
        }
        val repository = object : RequestRepository {
            override suspend fun getDetail(formId: String, responseId: String) = null
            override fun observeRequests() = flow {
                if (denied) throw com.uvg.cc3087.myapp.data.repository.RequestAccessDeniedException()
                emit(listOf(row))
            }
        }
        val model = ResponseDashboardViewModel(repository, SavedStateHandle(), clock) { zone }
            .also { store.put("calendar", it) }
        runCurrent()
        assertTrue(model.uiState.value.dashboard.hasUnread)
        denied = true
        model.refresh()
        runCurrent()
        instant = now.plusSeconds(86_400)
        model.checkCalendar()
        zone = ZoneId.of("Asia/Tokyo")
        model.checkCalendar()
        model.selectPeriod(ResponsePeriod.ALL)
        assertEquals(RequestListLoadStatus.FORBIDDEN, model.uiState.value.status)
        assertFalse(model.uiState.value.hasData)
        assertFalse(model.uiState.value.isRefreshing)
        assertTrue(model.uiState.value.dashboard.requests.isEmpty())
        assertTrue(model.uiState.value.dashboard.forms.isEmpty())
        assertEquals(0, model.uiState.value.dashboard.unreadCount)
    }

    @Test fun cachedFailureRemainsVisibleWhenChangingTheFilters() = runTest {
        val repository = Repository()
        repository.rows.value = listOf(row)
        val model = model(repository)
        runCurrent()
        repository.fail = true
        model.refresh()
        runCurrent()
        model.selectForm("f")
        model.selectPeriod(ResponsePeriod.ALL)
        assertEquals(RequestListLoadStatus.ERROR, model.uiState.value.status)
        assertTrue(model.uiState.value.hasData)
        assertEquals(listOf(row), model.uiState.value.dashboard.requests)
        assertFalse(model.uiState.value.isRefreshing)
    }

    @Test fun refreshKeepsTheLastResultsWhileWaitingForTheNextEmission() = runTest {
        val gate = CompletableDeferred<Unit>()
        var subscriptions = 0
        val repository = object : RequestRepository {
            override suspend fun getDetail(formId: String, responseId: String) = null
            override fun observeRequests() = flow {
                if (++subscriptions > 1) gate.await()
                emit(listOf(row))
            }
        }
        val model = model(repository)
        runCurrent()
        model.refresh()
        runCurrent()
        assertTrue(model.uiState.value.isRefreshing)
        assertTrue(model.uiState.value.hasData)
        assertEquals(RequestListLoadStatus.CONTENT, model.uiState.value.status)
        assertEquals(listOf(row), model.uiState.value.dashboard.requests)
        model.selectPeriod(ResponsePeriod.ALL)
        assertTrue(model.uiState.value.isRefreshing)
        gate.complete(Unit)
        runCurrent()
        assertFalse(model.uiState.value.isRefreshing)
        assertEquals(RequestListLoadStatus.CONTENT, model.uiState.value.status)
    }
}
