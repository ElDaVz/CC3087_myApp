package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.domain.requests.*
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.ZoneId

data class ResponseDashboardUiState(
    val status: RequestListLoadStatus = RequestListLoadStatus.LOADING,
    val filter: ResponseFilter = ResponseFilter(),
    val dashboard: ResponseDashboard = ResponseDashboard(),
    val hasData: Boolean = false
)

class ResponseDashboardViewModel(
    private val repository: RequestRepository,
    private val saved: SavedStateHandle,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = ZoneId::systemDefault
) : ViewModel() {
    private var source = emptyList<RequestSummary>()
    private var hasData = false
    private var filter = ResponseFilter(saved["dashboard_form"],
        ResponsePeriod.entries.find { it.name == saved.get<String>("dashboard_period") } ?: ResponsePeriod.MONTH)
    private val mutableState = MutableStateFlow(ResponseDashboardUiState(filter = filter))
    val uiState = mutableState.asStateFlow()
    private var revision = 0L
    private var job: Job? = null
    private var calendar = clock.instant().atZone(zone()).toLocalDate() to zone()
    init { observe() }

    fun selectForm(id: String?) {
        filter = filter.copy(formId = id); saved["dashboard_form"] = id; project()
    }
    fun selectPeriod(period: ResponsePeriod) {
        filter = filter.copy(period = period); saved["dashboard_period"] = period.name; project()
    }
    fun checkCalendar() {
        val next = clock.instant().atZone(zone()).toLocalDate() to zone()
        if (next != calendar) { calendar = next; project() }
    }
    fun refresh() = observe(requestSync = true)
    private fun project(status: RequestListLoadStatus = if (hasData) RequestListLoadStatus.CONTENT else RequestListLoadStatus.LOADING) {
        mutableState.value = ResponseDashboardUiState(status, filter, responseDashboard(source, filter, clock.instant(), zone()), hasData)
    }
    private fun observe(requestSync: Boolean = false) {
        val stamp = ++revision
        job?.cancel()
        project(if (hasData) RequestListLoadStatus.CONTENT else RequestListLoadStatus.LOADING)
        job = viewModelScope.launch {
            try {
                if (requestSync) repository.requestRefresh()
                repository.observeRequests().collect { rows ->
                    if (stamp == revision) { source = rows; hasData = true; project() }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (stamp == revision) project(when (error) {
                    is RequestAccessDeniedException -> RequestListLoadStatus.FORBIDDEN
                    is RequestBackendUnavailableException -> RequestListLoadStatus.UNAVAILABLE
                    else -> RequestListLoadStatus.ERROR
                })
            }
        }
    }
    companion object {
        fun factory(repository: RequestRepository) = viewModelFactory {
            initializer { ResponseDashboardViewModel(repository, createSavedStateHandle()) }
        }
    }
}
