package com.uvg.cc3087.myapp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.domain.requests.ResponseFilter
import com.uvg.cc3087.myapp.domain.requests.ResponseForm
import com.uvg.cc3087.myapp.ui.components.ResponseFilters
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import com.uvg.cc3087.myapp.ui.screens.RequestDetailScreen
import com.uvg.cc3087.myapp.ui.screens.RequestListScreen
import com.uvg.cc3087.myapp.ui.state.*
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ResponseInboxTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val first = RequestDetail("form", "shared", "Pedidos de prueba", Instant.now(), listOf(
        RequestDetailAnswer("name", "Nombre", RequestAnswerValue.Text("Cliente de prueba"))
    ), requestStatus = RequestStatus.PENDING)
    private val rows = listOf(first,
        first.copy(responseId = "reviewed", requestStatus = RequestStatus.COMPLETED),
        first.copy(formId = "other", formTitle = "Reservas de prueba", requestStatus = RequestStatus.IN_PROGRESS))

    private class Requests(private val details: List<RequestDetail>) : RequestRepository {
        val read = MutableStateFlow(setOf("form/reviewed"))
        var marks = 0
        override fun observeRequests() = read.map { seen ->
            details.map { it.toSummary().copy(isRead = "${it.formId}/${it.responseId}" in seen) }
        }
        override suspend fun getDetail(formId: String, responseId: String) =
            details.find { it.formId == formId && it.responseId == responseId }
        override suspend fun markRead(formId: String, responseId: String) {
            marks++
            read.value += "$formId/$responseId"
        }
    }

    @Test fun unreadOnlySurvivesDetailBackAndRestorationAndCanBeCleared() {
        val repository = Requests(rows)
        val restoration = openInbox(repository)
        compose.onNodeWithTag("requests-only-unread").performScrollTo().performClick()
        totalIs(2)
        compose.onNodeWithTag("request-list").performScrollToNode(hasTestTag("request-form/shared"))
        compose.onNodeWithTag("request-form/shared").performClick()
        compose.onNodeWithText("Detalle de solicitud").assertExists()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("request-list").performScrollToIndex(0)
        compose.onNodeWithTag("requests-only-unread").assertIsSelected()
        totalIs(1)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("request-list").performScrollToIndex(0)
        compose.onNodeWithTag("requests-only-unread").assertIsSelected()
        totalIs(1)
        compose.onNodeWithTag("requests-clear-filters").performScrollTo().performClick()
        totalIs(3)
        compose.onNodeWithTag("request-list").performScrollToIndex(0)
        compose.onNodeWithTag("requests-only-unread").assertIsNotSelected()
    }

    @Test fun anEmptyFilteredViewDoesNotClaimThatNoResponsesWereEverReceived() {
        openInbox(Requests(rows))
        compose.onNodeWithTag("status-COMPLETED").performScrollTo().performClick()
        compose.onNodeWithTag("requests-only-unread").performScrollTo().performClick()
        totalIs(0)
        compose.onNodeWithTag("request-list").performScrollToNode(hasText("No hay respuestas que coincidan con esta vista."))
        compose.onNodeWithText("No hay respuestas que coincidan con esta vista.").assertIsDisplayed()
        compose.onNodeWithText("Todavía no has recibido solicitudes.").assertDoesNotExist()
        compose.onNodeWithTag("request-list").performScrollToIndex(0)
        compose.onNodeWithTag("requests-clear-filters").performScrollTo().performClick()
        totalIs(3)
    }

    @Test fun viewingAndFilteringTheInboxDoesNotMarkResponsesRead() {
        val repository = Requests(rows)
        openInbox(repository)
        compose.onNodeWithTag("requests-only-unread").performScrollTo().performClick()
        totalIs(2)
        compose.onNodeWithTag("status-PENDING").performScrollTo().performClick()
        totalIs(1)
        compose.runOnIdle { assertEquals(0, repository.marks) }
    }

    @Test fun falseBlankChoicesAndAttachmentNamesRemainReadOnly() {
        val detail = first.copy(answers = listOf(
            RequestDetailAnswer("text", "Código recibido", RequestAnswerValue.Text("0042\ntexto original")),
            RequestDetailAnswer("choice", "Opciones recibidas", RequestAnswerValue.Choices(listOf("Sin azúcar", "Entrega durante la tarde"))),
            RequestDetailAnswer("boolean", "Confirmación", RequestAnswerValue.Checked(false)),
            RequestDetailAnswer("blank", "Notas", RequestAnswerValue.Text("")),
            RequestDetailAnswer("file", "Archivo histórico", RequestAnswerValue.Attachments(listOf(RequestAttachment("detalle.pdf", "private/reference"))))
        ))
        compose.setContent { SpanishTestTheme { RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.CONTENT, detail), {}, {}) } }
        for (value in listOf("0042\ntexto original", "Sin azúcar", "No", "Sin respuesta", "detalle.pdf")) {
            compose.onNodeWithTag("request-detail-list").performScrollToNode(hasText(value))
            compose.onNodeWithText(value).assertIsDisplayed()
        }
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("private/reference").assertDoesNotExist()
    }

    @Test fun aFailedStatusCanBeRetriedWithoutOptimisticallyChangingTheSelection() {
        var state by mutableStateOf(RequestDetailUiState(RequestDetailLoadStatus.CONTENT, first,
            statusSaveFailed = true, failedStatus = RequestStatus.COMPLETED))
        var retries = 0
        compose.setContent {
            SpanishTestTheme {
                RequestDetailScreen(state, {}, {}, onRetryStatus = {
                    retries++
                    state = state.copy(savingStatus = true, statusSaveFailed = false, failedStatus = null)
                })
            }
        }
        compose.onNodeWithTag("request-detail-list").performScrollToNode(hasTestTag("retry-request-status"))
        compose.onNodeWithTag("retry-request-status").performClick()
        compose.onNodeWithTag("detail-status-PENDING").assertIsSelected().assertIsNotEnabled()
        compose.onNodeWithTag("detail-status-COMPLETED").assertIsNotSelected().assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, retries)
            state = state.copy(detail = first.copy(requestStatus = RequestStatus.COMPLETED), savingStatus = false)
        }
        compose.onNodeWithTag("detail-status-COMPLETED").assertIsSelected().assertIsEnabled()
    }

    @Test fun unknownHistoricalStatusDoesNotCreateAnEditableStatus() {
        val detail = first.copy(requestStatus = null, statusLabel = "Estado importado")
        compose.setContent { SpanishTestTheme { RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.CONTENT, detail), {}, {}) } }
        compose.onNodeWithText("Estado: Estado importado").assertExists()
        compose.onNodeWithTag("detail-status-PENDING").assertDoesNotExist()
        compose.onNodeWithText("Las respuestas enviadas son de solo lectura.").assertExists()
    }

    @Test fun forbiddenDetailNeverRendersACachedSnapshot() {
        compose.setContent {
            SpanishTestTheme { RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.FORBIDDEN, first), {}, {}) }
        }
        compose.onNodeWithText("No tienes permiso para ver esta solicitud.").assertExists()
        compose.onNodeWithText(first.formTitle).assertDoesNotExist()
        compose.onNodeWithText("Cliente de prueba").assertDoesNotExist()
    }

    @Test fun listAndDetailSupportLightDarkAndLargeText() {
        var dark by mutableStateOf(false)
        var scale by mutableStateOf(1f)
        var detailOpen by mutableStateOf(false)
        compose.setContent {
            SpanishTestTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                    MyappTheme(darkTheme = dark, dynamicColor = false) {
                        if (detailOpen) RequestDetailScreen(
                            RequestDetailUiState(RequestDetailLoadStatus.CONTENT, first), { detailOpen = false }, {}
                        ) else RequestListScreen(
                            RequestListUiState(RequestListLoadStatus.CONTENT, rows.map { it.toSummary() }, sourceEmpty = false),
                            { detailOpen = true }, {}, {}, rememberLazyListState(),
                            filters = { ResponseFilters(ResponseFilter(), listOf(ResponseForm("form", first.formTitle)), {}, {}) }
                        )
                    }
                }
            }
        }
        for ((name, useDark, fontScale) in listOf(Triple("light", false, 1f), Triple("dark", true, 1f), Triple("large", true, 1.5f))) {
            compose.runOnIdle { dark = useDark; scale = fontScale; detailOpen = false }
            compose.onNodeWithTag("request-list").performScrollToNode(hasTestTag("request-form/shared"))
            capture("$name-list")
            compose.onNodeWithTag("request-form/shared").performClick()
            compose.onNodeWithTag("request-detail-list").performScrollToIndex(0)
            capture("$name-detail")
            compose.onNodeWithTag("request-detail-list").performScrollToNode(hasText("Cliente de prueba"))
            compose.onNodeWithText("Cliente de prueba").assertIsDisplayed()
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
            capture("$name-answer")
        }
    }

    private fun openInbox(repository: RequestRepository): StateRestorationTester = StateRestorationTester(compose).also {
        val forms = SampleFormRepository()
        val templates = SampleTemplateRepository()
        it.setContent { SpanishTestTheme { FormLinkRoute(forms, templates, repository) } }
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("metric-total").performScrollTo().performClick()
    }

    private fun totalIs(count: Int) {
        compose.onNodeWithTag("request-list").performScrollToNode(hasTestTag("requests-visible-total"))
        compose.onNodeWithTag("requests-visible-total").assertTextContains("Total: $count")
    }

    private fun capture(name: String) {
        // las capturas usan únicamente los datos de prueba definidos en esta clase
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "responses-$name.png")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val command = "cp " + file.absolutePath + " /sdcard/Download/formlink-responses-" + name + ".png"
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
}
