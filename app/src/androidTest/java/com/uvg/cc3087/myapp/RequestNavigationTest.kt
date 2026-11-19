package com.uvg.cc3087.myapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.uvg.cc3087.myapp.data.model.RequestAnswerValue
import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.model.RequestDetailAnswer
import com.uvg.cc3087.myapp.data.model.toSummary
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.data.repository.UnavailableRequestRepository
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class RequestNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun responsesOpensTheSelectedDetailAndBothBackActionsReturnToTheList() {
        launchApp(TestRequests(listOf(request("a"), request("b"))))
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("metric-total").performScrollTo().performClick()
        compose.onNodeWithText("Solicitudes recibidas").assertExists()
        openRequest("a")
        assertAnswerDisplayed("a")
        dispatchBack()
        openRequest("b")
        assertAnswerDisplayed("b")
        compose.onNodeWithText("Respuesta de a").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Solicitudes recibidas").assertExists()
        compose.onNodeWithText("Formularios").performClick()
        compose.onNodeWithText("Mis formularios").assertExists()
    }

    @Test fun selectedRequestSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        launchApp(TestRequests(listOf(request("a"), request("b"))), restoration)
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("metric-total").performScrollTo().performClick()
        openRequest("b")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Detalle de solicitud").assertExists()
        assertAnswerDisplayed("b")
        dispatchBack()
        compose.onNodeWithText("Solicitudes recibidas").assertExists()
    }

    @Test fun backFromDetailPreservesListScrollPosition() {
        launchApp(TestRequests((1..30).map { request("form-$it") }))
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("metric-total").performScrollTo().performClick()
        openRequest("form-25")
        assertAnswerDisplayed("form-25")
        dispatchBack()
        compose.onNodeWithTag("request-form-25/shared").assertIsDisplayed()
    }

    @Test fun unavailableSourceDoesNotDisplayExampleCustomersOrAnEmptyInbox() {
        launchApp(UnavailableRequestRepository)
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithText("Las solicitudes todavía no están disponibles en esta versión.").assertExists()
        compose.onNodeWithText("Todavía no has recibido solicitudes.").assertDoesNotExist()
        compose.onNodeWithText("Solicitudes de ejemplo. No hay datos de clientes reales conectados.").assertDoesNotExist()
        dispatchBack()
        compose.onNodeWithText("Mis formularios").assertExists()
    }

    @Test fun emptyInboxAndTemplateTabAreConnected() {
        launchApp(TestRequests(emptyList()))
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasText("Todavía no has recibido solicitudes."))
        compose.onNodeWithText("Todavía no has recibido solicitudes.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Mis formularios").assertExists()
    }

    private fun openRequest(form: String) {
        // Lazy rows may not exist in the semantics tree until the list scrolls to them.
        val tag = "request-$form/shared"
        compose.onNodeWithTag("request-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
    }

    private fun assertAnswerDisplayed(form: String) {
        val answer = "Respuesta de $form"
        compose.onNodeWithTag("request-detail-list").performScrollToNode(hasText(answer))
        compose.onNodeWithText(answer).assertIsDisplayed()
    }

    private fun dispatchBack() {
        // Exercise the Activity's BackHandler dispatch without Espresso's window-focus lookup.
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun request(form: String) = RequestDetail(
        form, "shared", "Formulario $form", Instant.now(),
        listOf(RequestDetailAnswer("answer", "Respuesta", RequestAnswerValue.Text("Respuesta de $form")))
    )

    private class TestRequests(val requests: List<RequestDetail>) : RequestRepository {
        override suspend fun getRequests() = requests.map { it.toSummary() }
        override suspend fun getDetail(formId: String, responseId: String) =
            requests.find { it.formId == formId && it.responseId == responseId }
    }

    private fun launchApp(repository: RequestRepository, restoration: StateRestorationTester? = null) {
        val forms = SampleFormRepository()
        val templates = SampleTemplateRepository()
        val content: @Composable () -> Unit = {
            SpanishTestTheme { FormLinkRoute(forms, templates, repository) }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
    }
}
