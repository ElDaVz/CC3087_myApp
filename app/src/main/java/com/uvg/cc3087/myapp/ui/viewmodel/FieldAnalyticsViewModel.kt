package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.domain.requests.*
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.ZoneId

data class FieldAnalyticsUiState(
    val formId: String? = null,
    val period: ResponsePeriod = ResponsePeriod.MONTH,
    val status: RequestListLoadStatus = RequestListLoadStatus.EMPTY,
    val analytics: FormAnalytics = FormAnalytics()
)

class FieldAnalyticsViewModel(
    private val repository: RequestRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val mutableState = MutableStateFlow(FieldAnalyticsUiState())
    val uiState = mutableState.asStateFlow()
    private var job: Job? = null
    private var revision = 0L
    private var selection: Pair<String?, ResponsePeriod>? = null
    private var hasData = false
    private var calendar = clock.instant().atZone(zone()).toLocalDate() to zone()

    fun select(formId: String?, period: ResponsePeriod, force: Boolean = false) {
        if (!force && selection == (formId to period)) return
        selection = formId to period
        val stamp = ++revision
        job?.cancel()
        hasData = false
        mutableState.value = FieldAnalyticsUiState(formId, period,
            if (formId == null) RequestListLoadStatus.EMPTY else RequestListLoadStatus.LOADING)
        if (formId == null) return
        job = viewModelScope.launch {
            try {
                repository.observeDetails(formId).collect { snapshots ->
                    val result = withContext(calculationDispatcher) { formAnalytics(formId, snapshots, period, clock.instant(), zone()) }
                    if (stamp == revision) {
                        hasData = true
                        mutableState.value = FieldAnalyticsUiState(formId, period, RequestListLoadStatus.CONTENT, result)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (stamp == revision) {
                    hasData = false
                    mutableState.value = FieldAnalyticsUiState(formId, period, when (error) {
                        is RequestAccessDeniedException -> RequestListLoadStatus.FORBIDDEN
                        is RequestBackendUnavailableException -> RequestListLoadStatus.UNAVAILABLE
                        else -> RequestListLoadStatus.ERROR
                    })
                }
            }
        }
    }

    fun retry() { selection?.let { select(it.first, it.second, force = true) } }

    fun checkCalendar() {
        val next = clock.instant().atZone(zone()).toLocalDate() to zone()
        if (calendar != next) {
            calendar = next
            // volver a observar conserva la cancelación y no permite que gane un resultado del día anterior
            if (hasData) retry()
        }
    }

    companion object {
        fun factory(repository: RequestRepository) = viewModelFactory { initializer { FieldAnalyticsViewModel(repository) } }
    }
}
