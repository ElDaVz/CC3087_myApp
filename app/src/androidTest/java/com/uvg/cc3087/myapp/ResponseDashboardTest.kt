package com.uvg.cc3087.myapp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ResponseDashboardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class Requests : RequestRepository {
        val details = listOf(
            RequestDetail("f", "new", "Pedidos", Instant.now(), emptyList(), requestStatus = RequestStatus.PENDING),
            RequestDetail("old", "past", "Histórico", Instant.parse("2020-01-01T12:00:00Z"), emptyList(), requestStatus = RequestStatus.COMPLETED))
        val read = MutableStateFlow(emptySet<String>())
        override fun observeRequests() = read.map { seen -> details.map { it.toSummary().copy(isRead = it.responseId in seen) } }
        override suspend fun getDetail(formId: String, responseId: String) = details.find { it.formId == formId && it.responseId == responseId }
        override suspend fun markRead(formId: String, responseId: String) { read.value += responseId }
    }
    private fun launch(repo: RequestRepository = Requests()) {
        compose.setContent { SpanishTestTheme { FormLinkRoute(SampleFormRepository(), SampleTemplateRepository(), repo) } }
    }
    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun dashboardFiltersInboxAndUnreadBadgeRespectTheWholeAccount() {
        launch()
        compose.onNodeWithTag("responses-unread-badge", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithText("Dashboard de respuestas").assertExists()
        compose.onNodeWithTag("metric-total").assertTextContains("1")
        compose.onNodeWithTag("metric-PENDING").performScrollTo().performClick()
        compose.onNodeWithTag("status-PENDING").assertIsSelected()
        compose.onNodeWithTag("request-f/new").performScrollTo().performClick()
        compose.onNodeWithText("Detalle de solicitud").assertExists()
        back()
        compose.onNodeWithText("Solicitudes recibidas").assertExists()
        // The old response is outside this period but must still keep the global badge.
        compose.onNodeWithTag("responses-unread-badge", useUnmergedTree = true).assertExists()
        back()
        compose.onNodeWithTag("period-ALL").performScrollTo().performClick()
        compose.onNodeWithTag("metric-total").assertTextContains("2")
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasTestTag("request-old/past"))
        compose.onNodeWithTag("request-old/past").performClick()
        compose.onNodeWithText("Detalle de solicitud").assertExists()
        back()
        compose.onNodeWithText("Dashboard de respuestas").assertExists()
        compose.onNodeWithTag("responses-unread-badge", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun formAndPeriodSelectionSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        val requests = Requests()
        restoration.setContent { SpanishTestTheme { FormLinkRoute(SampleFormRepository(), SampleTemplateRepository(), requests) } }
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("period-ALL").performScrollTo().performClick()
        compose.onNodeWithTag("dashboard-form-filter").performClick()
        compose.onNodeWithText("Histórico").performClick()
        compose.onNodeWithTag("metric-total").performScrollTo().performClick()
        compose.onNodeWithTag("request-old/past").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Detalle de solicitud").assertExists()
        back()
        compose.onNodeWithText("Solicitudes recibidas").assertExists()
        compose.onNodeWithTag("period-ALL").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("dashboard-form-filter").assertTextContains("Histórico")
        compose.onNodeWithTag("request-f/new").assertDoesNotExist()
    }

    @Test fun presentationSupportsLightDarkAndLargeText() {
        val dark = androidx.compose.runtime.mutableStateOf(false)
        val scale = androidx.compose.runtime.mutableStateOf(1f)
        val data = com.uvg.cc3087.myapp.domain.requests.responseDashboard(
            Requests().details.map { it.toSummary() }, com.uvg.cc3087.myapp.domain.requests.ResponseFilter(), Instant.now(), java.time.ZoneId.systemDefault())
        compose.setContent { SpanishTestTheme {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, scale.value)) {
                com.uvg.cc3087.myapp.ui.theme.MyappTheme(darkTheme = dark.value, dynamicColor = false) {
                    com.uvg.cc3087.myapp.ui.screens.ResponseDashboardScreen(
                        com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardUiState(com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus.CONTENT,
                            dashboard = data, hasData = true), androidx.compose.foundation.lazy.rememberLazyListState(), {}, {}, {}, {}, {}, {}, {})
                }
            }
        } }
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        for ((name, useDark, fontScale) in listOf(Triple("light", false, 1f), Triple("dark", true, 1f), Triple("large", true, 1.5f))) {
            compose.runOnIdle { dark.value = useDark; scale.value = fontScale }
            compose.onNodeWithTag("response-dashboard").performScrollToNode(hasTestTag("open-inbox"))
            compose.onNodeWithTag("open-inbox").assertIsDisplayed()
            compose.onNodeWithTag("response-dashboard").performScrollToIndex(0)
            // Capture synthetic UI only; the shell copies artifacts outside app storage before test cleanup.
            val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "dashboard-$name.png")
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            val command = "cp " + file.absolutePath + " /sdcard/Download/formlink-dashboard-" + name + ".png"
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        }
    }

    @Test fun filteredEmptyPeriodDoesNotClaimTheWholeInboxIsEmpty() {
        launch()
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("dashboard-form-filter").performClick()
        compose.onNodeWithText("Histórico").performClick()
        compose.onNodeWithText("No hay respuestas para estos filtros.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Todavía no has recibido solicitudes.").assertDoesNotExist()
        compose.onNodeWithTag("period-ALL").performScrollTo().performClick()
        compose.onNodeWithTag("metric-total").assertTextContains("1")
    }
}
