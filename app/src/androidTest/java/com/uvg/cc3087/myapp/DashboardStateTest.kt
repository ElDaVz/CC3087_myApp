package com.uvg.cc3087.myapp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.domain.requests.ResponseFilter
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.domain.requests.responseDashboard
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import com.uvg.cc3087.myapp.ui.screens.ResponseDashboardScreen
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardUiState
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DashboardStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.parse("2026-11-18T18:00:00Z")
    private val rows = listOf(
        RequestSummary("form", "pending", "Reservas", now, requestStatus = RequestStatus.PENDING),
        RequestSummary("form", "done", "Reservas", now, requestStatus = RequestStatus.COMPLETED, isRead = true),
        RequestSummary("form", "unknown", "Reservas", null)
    )
    private val data = responseDashboard(rows, ResponseFilter(period = ResponsePeriod.ALL), now, ZoneOffset.UTC)
    private val content = ResponseDashboardUiState(RequestListLoadStatus.CONTENT,
        ResponseFilter(period = ResponsePeriod.ALL), data, hasData = true)

    @Test fun initialFailureOffersRetryWithoutPretendingTheInboxIsEmpty() {
        var retries = 0
        show(ResponseDashboardUiState(status = RequestListLoadStatus.ERROR)) { retries++ }
        compose.onNodeWithText("No se pudieron cargar las solicitudes. Inténtalo de nuevo.").assertIsDisplayed()
        compose.onNodeWithText("Todavía no has recibido solicitudes.").assertDoesNotExist()
        compose.onNodeWithTag("metric-total").assertDoesNotExist()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun forbiddenStateNeverDisplaysCachedCustomerSummaries() {
        show(content.copy(status = RequestListLoadStatus.FORBIDDEN))
        compose.onNodeWithText("No tienes permiso para consultar estas solicitudes. Revisa tu cuenta e inténtalo de nuevo.").assertExists()
        compose.onNodeWithTag("metric-total").assertDoesNotExist()
        compose.onNodeWithTag("dashboard-form-filter").assertDoesNotExist()
        compose.onNodeWithText("Reservas").assertDoesNotExist()
        compose.onNodeWithTag("responses-unread-badge", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun refreshingAndCachedFailureKeepTheCountsAndExplainTheirState() {
        var state by mutableStateOf(content.copy(isRefreshing = true))
        compose.setContent {
            SpanishTestTheme {
                ResponseDashboardScreen(state, rememberLazyListState(), {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("dashboard-refreshing").assertExists()
        compose.onNodeWithContentDescription("Actualizar solicitudes").assertIsNotEnabled()
        compose.onNodeWithTag("metric-total").performScrollTo().assertTextContains("3")
        compose.onNodeWithText("Cargando solicitudes…").assertDoesNotExist()
        compose.runOnIdle { state = content.copy(status = RequestListLoadStatus.ERROR) }
        compose.onNodeWithTag("response-dashboard").performScrollToIndex(0)
        compose.onNodeWithText("No se pudieron actualizar las respuestas. Se muestra el último resultado disponible.").assertExists()
        compose.onNodeWithContentDescription("Actualizar solicitudes").assertIsEnabled()
        compose.onNodeWithTag("metric-total").performScrollTo().assertTextContains("3")
    }

    @Test fun historyActionClearsTheFiltersAndFindsOlderResponses() {
        val repository = object : RequestRepository {
            override suspend fun getDetail(formId: String, responseId: String) = null
            override fun observeRequests() = flowOf(listOf(rows.first().copy(submittedAt = Instant.parse("2020-01-01T12:00:00Z"))))
        }
        compose.setContent {
            SpanishTestTheme { FormLinkRoute(SampleFormRepository(), SampleTemplateRepository(), repository) }
        }
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasText("Ver todo el historial"))
        compose.onNodeWithText("Ver todo el historial").performClick()
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasTestTag("period-ALL"))
        compose.onNodeWithTag("period-ALL").assertIsSelected()
        compose.onNodeWithTag("metric-total").performScrollTo().assertTextContains("1")
        compose.onNodeWithText("No hay respuestas para estos filtros.").assertDoesNotExist()
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasTestTag("response-chart"))
        compose.onNode(hasContentDescription(": 1 respuestas", substring = true)).assertExists()
    }

    @Test fun undatedResponsesAreCountedWithoutDrawingAnInventedChart() {
        val undated = responseDashboard(listOf(rows.last()), ResponseFilter(period = ResponsePeriod.ALL), now, ZoneOffset.UTC)
        show(content.copy(dashboard = undated))
        compose.onNodeWithTag("metric-total").assertTextContains("1")
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasText("No hay respuestas con fecha para graficar con estos filtros."))
        compose.onNodeWithText("No hay respuestas con fecha para graficar con estos filtros.").assertIsDisplayed()
        compose.onNodeWithTag("response-chart").assertDoesNotExist()
        compose.onNodeWithText("Todavía no has recibido solicitudes.").assertDoesNotExist()
    }

    @Test fun completionIncludesAllVisibleStatusesAndUnreadCountUsesTheSameFilter() {
        show(content)
        compose.onNodeWithTag("dashboard-unread-count").assertTextContains("Sin revisar: 2")
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasTestTag("dashboard-completion-progress"))
        compose.onNodeWithText("Completadas: 1 de 3").assertExists()
        compose.onNodeWithTag("dashboard-completion-progress").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(1f / 3f, 0f..1f)
        ))
    }

    @Test fun cardsUseFourColumnsInWideSpaceAndOneWithNarrowLargeText() {
        var compact by mutableStateOf(false)
        compose.setContent {
            SpanishTestTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, if (compact) 1.5f else 1f)) {
                    Box(Modifier.width(if (compact) 320.dp else 840.dp)) {
                        ResponseDashboardScreen(content, rememberLazyListState(), {}, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        val total = compose.onNodeWithTag("metric-total").fetchSemanticsNode().boundsInRoot
        val completed = compose.onNodeWithTag("metric-COMPLETED").fetchSemanticsNode().boundsInRoot
        assertEquals(total.top, completed.top, 1f)
        assertEquals(total.height, completed.height, 1f)
        compose.runOnIdle { compact = true }
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasTestTag("metric-PENDING"))
        val compactTotal = compose.onNodeWithTag("metric-total").fetchSemanticsNode().boundsInRoot
        val pending = compose.onNodeWithTag("metric-PENDING").fetchSemanticsNode().boundsInRoot
        assertEquals(compactTotal.left, pending.left, 1f)
        assertTrue(pending.top >= compactTotal.bottom)
    }

    @Test fun aMissingFormSelectionIsNotLabelledAsAllForms() {
        compose.setContent {
            SpanishTestTheme {
                com.uvg.cc3087.myapp.ui.components.ResponseFilters(
                    ResponseFilter(formId = "missing"), data.forms, {}, {}
                )
            }
        }
        compose.onNodeWithTag("dashboard-form-filter")
            .assertTextContains("Formulario seleccionado no disponible")
        compose.onNodeWithText("Todos los formularios").assertDoesNotExist()
    }

    private fun show(state: ResponseDashboardUiState, onRetry: () -> Unit = {}) {
        compose.setContent {
            SpanishTestTheme {
                ResponseDashboardScreen(state, rememberLazyListState(), {}, {}, {}, {}, {}, {}, onRetry)
            }
        }
    }
}
