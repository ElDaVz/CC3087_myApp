package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class FieldAnalyticsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun clear() { store.clear() }
    private val now = Instant.parse("2026-11-19T18:00:00Z")
    private val row = RequestDetail("f", "one", "Form", now, listOf(
        RequestDetailAnswer("q", "Pregunta", RequestAnswerValue.Checked(false), RequestFieldSnapshot(FormFieldType.CHECKBOX))))
    private class Repository : RequestRepository {
        val rows = MutableStateFlow(emptyList<RequestDetail>())
        var error: Exception? = null
        var observations = 0
        var marks = 0
        override suspend fun getDetail(formId: String, responseId: String) = null
        override suspend fun markRead(formId: String, responseId: String) { marks++ }
        override fun observeDetails(formId: String) = flow {
            observations++
            error?.let { throw it }
            emitAll(rows.map { data -> data.filter { it.formId == formId } })
        }
    }
    private fun model(repository: RequestRepository, clock: Clock = Clock.fixed(now, ZoneOffset.UTC)) =
        FieldAnalyticsViewModel(repository, clock, { ZoneOffset.UTC }, main.dispatcher).also { store.put("analytics", it) }

    @Test fun noFormSelectedDoesNotReadAnyAnswers() = runTest {
        val repo = Repository(); val model = model(repo)
        model.select(null, ResponsePeriod.ALL); runCurrent()
        assertEquals(0, repo.observations)
        assertNull(model.uiState.value.formId)
    }

    @Test fun streamsMetricsWithoutMarkingAnyResponseRead() = runTest {
        val repo = Repository(); val model = model(repo)
        model.select("f", ResponsePeriod.ALL); repo.rows.value = listOf(row); runCurrent()
        assertEquals(1, model.uiState.value.analytics.responseCount)
        repo.rows.value = listOf(row, row.copy(responseId = "two")); runCurrent()
        assertEquals(2, model.uiState.value.analytics.responseCount)
        assertEquals(0, repo.marks)
    }

    @Test fun switchingFormsClearsAnswersImmediatelyAndCancelsTheOldObservation() = runTest {
        val repo = Repository(); val model = model(repo)
        repo.rows.value = listOf(row)
        model.select("f", ResponsePeriod.ALL); runCurrent()
        model.select("other", ResponsePeriod.ALL)
        assertTrue(model.uiState.value.analytics.fields.isEmpty())
        assertEquals(RequestListLoadStatus.LOADING, model.uiState.value.status)
        runCurrent()
        assertEquals(1, repo.rows.subscriptionCount.value)
        assertEquals(0, model.uiState.value.analytics.responseCount)
        model.select(null, ResponsePeriod.ALL); runCurrent()
        assertEquals(0, repo.rows.subscriptionCount.value)
    }

    @Test fun errorsClearOldMetricsAndRetryCanRecover() = runTest {
        val repo = Repository(); val model = model(repo)
        repo.rows.value = listOf(row); model.select("f", ResponsePeriod.ALL); runCurrent()
        repo.error = java.io.IOException(); model.retry(); runCurrent()
        assertEquals(RequestListLoadStatus.ERROR, model.uiState.value.status)
        assertTrue(model.uiState.value.analytics.fields.isEmpty())
        repo.error = null; model.retry(); runCurrent()
        assertEquals(1, model.uiState.value.analytics.responseCount)
    }

    @Test fun deniedAndUnavailableSourcesNeverRenderCachedMetrics() = runTest {
        for ((error, status) in listOf(RequestAccessDeniedException() to RequestListLoadStatus.FORBIDDEN,
            RequestBackendUnavailableException() to RequestListLoadStatus.UNAVAILABLE)) {
            val repo = Repository(); val model = model(repo)
            repo.rows.value = listOf(row); model.select("f", ResponsePeriod.ALL); runCurrent()
            repo.error = error; model.retry(); runCurrent()
            assertEquals(status, model.uiState.value.status)
            assertTrue(model.uiState.value.analytics.fields.isEmpty())
        }
    }

    @Test fun periodChangesAndCalendarRolloverUseTheSameWindowAsTheDashboard() = runTest {
        var instant = now
        val clock = object : Clock() {
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId) = this
            override fun instant() = instant
        }
        val repo = Repository(); val model = model(repo, clock)
        repo.rows.value = listOf(row.copy(submittedAt = Instant.parse("2026-11-13T00:00:00Z")))
        model.select("f", ResponsePeriod.WEEK); runCurrent()
        assertEquals(1, model.uiState.value.analytics.responseCount)
        instant = instant.plusSeconds(86400); model.checkCalendar(); runCurrent()
        assertEquals(0, model.uiState.value.analytics.responseCount)
        model.select("f", ResponsePeriod.ALL); runCurrent()
        assertEquals(1, model.uiState.value.analytics.responseCount)
    }

    @Test fun disposedSessionStopsReadingAndNeverAcceptsLaterRows() = runTest {
        val repo = Repository(); val model = model(repo)
        repo.rows.value = listOf(row); model.select("f", ResponsePeriod.ALL); runCurrent()
        store.clear(); runCurrent()
        assertEquals(0, repo.rows.subscriptionCount.value)
        repo.rows.value = emptyList(); runCurrent()
        assertEquals(1, model.uiState.value.analytics.responseCount)
    }

    @Test fun unexpectedOwnerDataFailsClosedInsteadOfCountingAnotherForm() = runTest {
        val bad = object : RequestRepository {
            override suspend fun getDetail(formId: String, responseId: String) = null
            override fun observeDetails(formId: String) = flowOf(listOf(row.copy(formId = "other")))
        }
        val model = model(bad); model.select("f", ResponsePeriod.ALL); runCurrent()
        assertEquals(RequestListLoadStatus.ERROR, model.uiState.value.status)
        assertTrue(model.uiState.value.analytics.fields.isEmpty())
    }
}
