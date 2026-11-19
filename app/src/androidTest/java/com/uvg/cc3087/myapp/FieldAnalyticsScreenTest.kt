package com.uvg.cc3087.myapp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.domain.requests.*
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import com.uvg.cc3087.myapp.ui.screens.ResponseDashboardScreen
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FieldAnalyticsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.now()
    private val labels = listOf("Nombre", "Comentario", "Cantidad", "Correo", "Confirmación", "Sabor", "Extras", "Entrega", "Fecha elegida")
    private fun detail(id: String, quantity: String) = RequestDetail("f", id, "Pedidos de prueba", now,
        FormFieldType.entries.map { type ->
            val value = when (type) {
                FormFieldType.CHECKBOX -> RequestAnswerValue.Checked(false)
                FormFieldType.CHECKBOX_GROUP -> RequestAnswerValue.Choices(listOf("Sin azúcar", "Para llevar"))
                FormFieldType.MULTIPLE_CHOICE, FormFieldType.DROPDOWN -> RequestAnswerValue.Choices(listOf("Sin azúcar"))
                FormFieldType.NUMBER -> RequestAnswerValue.Text(quantity)
                FormFieldType.EMAIL -> RequestAnswerValue.Text("privado@example.test")
                FormFieldType.DATE -> RequestAnswerValue.Text("2030-01-10 14:30")
                else -> RequestAnswerValue.Text("texto privado de prueba")
            }
            val label = when (type) {
                FormFieldType.TEXT -> labels[0]; FormFieldType.PARAGRAPH -> labels[1]
                FormFieldType.NUMBER -> labels[2]; FormFieldType.EMAIL -> labels[3]
                FormFieldType.CHECKBOX -> labels[4]; FormFieldType.MULTIPLE_CHOICE -> labels[5]
                FormFieldType.CHECKBOX_GROUP -> labels[6]; FormFieldType.DROPDOWN -> labels[7]
                FormFieldType.DATE -> labels[8]
            }
            RequestDetailAnswer(type.name, label, value, RequestFieldSnapshot(type,
                if (type.hasOptions) listOf("Sin azúcar", "Para llevar") else emptyList(),
                unit = if (type == FormFieldType.NUMBER) "kg" else "", includeTime = type == FormFieldType.DATE),
                if (type == FormFieldType.NUMBER) quantity.toBigDecimal() else null)
        }, requestStatus = RequestStatus.PENDING)
    private val details get() = listOf(detail("one", "1.5"), detail("two", "3.5"))
    private class Requests(initial: List<RequestDetail>) : RequestRepository {
        val rows = MutableStateFlow(initial)
        var marks = 0
        override fun observeRequests() = rows.map { list -> list.map { it.toSummary() } }
        override fun observeDetails(formId: String) = rows.map { list -> list.filter { it.formId == formId } }
        override suspend fun getDetail(formId: String, responseId: String) = rows.value.find { it.formId == formId && it.responseId == responseId }
        override suspend fun markRead(formId: String, responseId: String) { marks++ }
    }
    private fun selectForm(title: String) {
        compose.onNodeWithTag("response-dashboard").performScrollToIndex(0)
        compose.onNodeWithTag("dashboard-form-filter").performClick()
        compose.onNodeWithText(title, useUnmergedTree = true).performClick()
    }
    private fun scrollTo(matcher: SemanticsMatcher) = compose.onNodeWithTag("response-dashboard").performScrollToNode(matcher)

    @Test fun selectedFormShowsSpecializedMetricsWithoutReadingOrExposingCustomerText() {
        val repository = Requests(details)
        compose.setContent { SpanishTestTheme { FormLinkRoute(SampleFormRepository(), SampleTemplateRepository(), repository) } }
        compose.onNodeWithText("Respuestas").performClick()
        scrollTo(hasText("Selecciona un formulario arriba para ver las métricas de cada componente."))
        selectForm("Pedidos de prueba")
        scrollTo(hasTestTag("field-analytics-total"))
        compose.onNodeWithTag("field-analytics-total").assertTextEquals("Respuestas en este período: 2")
        scrollTo(hasText("Cantidad"))
        compose.onNodeWithText("Unidad: kg").assertExists()
        compose.onNodeWithText("2,5").assertExists()
        scrollTo(hasText("Correo"))
        compose.onNodeWithText("Direcciones con formato válido: 2").assertExists()
        compose.onNodeWithText("privado@example.test").assertDoesNotExist()
        compose.onNodeWithText("texto privado de prueba").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, repository.marks) }
    }

    @Test fun switchingFormsNeverKeepsThePreviousQuestions() {
        val other = detail("other", "10").copy(formId = "other", formTitle = "Reservas", answers = listOf(
            detail("other", "10").answers.first().copy(label = "Otra pregunta")))
        val repository = Requests(details + other)
        compose.setContent { SpanishTestTheme { FormLinkRoute(SampleFormRepository(), SampleTemplateRepository(), repository) } }
        compose.onNodeWithText("Respuestas").performClick()
        selectForm("Pedidos de prueba")
        scrollTo(hasText("Cantidad"))
        selectForm("Reservas")
        scrollTo(hasText("Otra pregunta"))
        compose.onNodeWithText("Cantidad").assertDoesNotExist()
        compose.onNodeWithTag("field-analytics-total").assertTextEquals("Respuestas en este período: 1")
    }

    @Test fun forbiddenDashboardDoesNotRenderCachedQuestionMetrics() {
        val filter = ResponseFilter("f")
        val data = responseDashboard(details.map { it.toSummary() }, filter, now, ZoneId.systemDefault())
        val metrics = FieldAnalyticsUiState("f", filter.period, RequestListLoadStatus.CONTENT,
            formAnalytics("f", details, filter.period, now, ZoneId.systemDefault()))
        compose.setContent { SpanishTestTheme {
            ResponseDashboardScreen(ResponseDashboardUiState(RequestListLoadStatus.FORBIDDEN, filter, data, hasData = true),
                rememberLazyListState(), {}, {}, {}, {}, {}, {}, {}, analyticsState = metrics)
        } }
        compose.onNodeWithTag("field-analytics-heading").assertDoesNotExist()
        compose.onNodeWithText("Cantidad").assertDoesNotExist()
        compose.onNodeWithText("No tienes permiso para consultar estas solicitudes. Revisa tu cuenta e inténtalo de nuevo.").assertExists()
    }

    @Test fun aMismatchedCachedFormNeverAppearsWhileTheNextSelectionLoads() {
        val data = responseDashboard(details.map { it.toSummary() }, ResponseFilter("other"), now, ZoneId.systemDefault())
        val old = FieldAnalyticsUiState("f", ResponsePeriod.MONTH, RequestListLoadStatus.CONTENT,
            formAnalytics("f", details, ResponsePeriod.MONTH, now, ZoneId.systemDefault()))
        compose.setContent { SpanishTestTheme {
            ResponseDashboardScreen(ResponseDashboardUiState(RequestListLoadStatus.CONTENT, ResponseFilter("other"), data, true),
                rememberLazyListState(), {}, {}, {}, {}, {}, {}, {}, analyticsState = old)
        } }
        scrollTo(hasTestTag("field-analytics-heading"))
        compose.onNodeWithText("Cantidad").assertDoesNotExist()
        compose.onNodeWithTag("field-analytics-card-0").assertDoesNotExist()
    }

    @Test fun emptySelectionsDoNotPresentAZeroOverZeroPercentage() {
        val blank = detail("blank", "1").copy(answers = listOf(RequestDetailAnswer("choice", "Opción vacía",
            RequestAnswerValue.Choices(emptyList()), RequestFieldSnapshot(FormFieldType.MULTIPLE_CHOICE, listOf("A", "B")))))
        val filter = ResponseFilter("f")
        val data = responseDashboard(listOf(blank.toSummary()), filter, now, ZoneId.systemDefault())
        val metrics = FieldAnalyticsUiState("f", filter.period, RequestListLoadStatus.CONTENT,
            formAnalytics("f", listOf(blank), filter.period, now, ZoneId.systemDefault()))
        compose.setContent { SpanishTestTheme {
            ResponseDashboardScreen(ResponseDashboardUiState(RequestListLoadStatus.CONTENT, filter, data, true),
                rememberLazyListState(), {}, {}, {}, {}, {}, {}, {}, analyticsState = metrics)
        } }
        scrollTo(hasText("Opción vacía"))
        compose.onNodeWithText("Contestadas: 0 / 1").assertExists()
        compose.onNodeWithText("Sin valores compatibles").assertExists()
        compose.onAllNodesWithText("0 / 0", substring = true).assertCountEquals(0)
    }

    @Test fun allNinePanelsSupportLightDarkAndLargeText() {
        var dark by mutableStateOf(false)
        var fontScale by mutableStateOf(1f)
        val filter = ResponseFilter("f")
        val rows = details
        val data = responseDashboard(rows.map { it.toSummary() }, filter, now, ZoneId.systemDefault())
        val metrics = FieldAnalyticsUiState("f", filter.period, RequestListLoadStatus.CONTENT,
            formAnalytics("f", rows, filter.period, now, ZoneId.systemDefault()))
        compose.setContent { SpanishTestTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MyappTheme(darkTheme = dark, dynamicColor = false) {
                    ResponseDashboardScreen(ResponseDashboardUiState(RequestListLoadStatus.CONTENT, filter, data, true),
                        rememberLazyListState(), {}, {}, {}, {}, {}, {}, {}, analyticsState = metrics)
                }
            }
        } }
        for ((name, useDark, scale) in listOf(Triple("light", false, 1f), Triple("dark", true, 1f), Triple("large", true, 1.5f))) {
            compose.runOnIdle { dark = useDark; fontScale = scale }
            labels.forEach { label ->
                val titleTag = "field-analytics-question-${metrics.analytics.fields.indexOfFirst { it.label == label }}"
                scrollTo(hasTestTag(titleTag))
                compose.onNodeWithTag(titleTag).assertTextEquals(label).assertIsDisplayed()
                if (label in listOf("Cantidad", "Extras", "Fecha elegida")) capture("$name-${labels.indexOf(label)}")
            }
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        }
    }

    private fun capture(name: String) {
        // solo se capturan los datos ficticios definidos arriba
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "analytics-$name.png")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val command = "cp " + file.absolutePath + " /sdcard/Download/formlink-analytics-" + name + ".png"
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
}
