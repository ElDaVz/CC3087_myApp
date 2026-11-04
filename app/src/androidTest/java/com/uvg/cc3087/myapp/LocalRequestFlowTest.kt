package com.uvg.cc3087.myapp

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import com.uvg.cc3087.myapp.ui.navigation.SubmitRequestRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LocalRequestFlowTest {
    private lateinit var db: FormLinkDatabase
    private lateinit var forms: RoomFormRepository
    private lateinit var requests: RoomRequestRepository
    private val databaseRule = object : ExternalResource() {
        override fun before() {
            db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, FormLinkDatabase::class.java).build()
            forms = RoomFormRepository(db.drafts(), db.requests())
            requests = RoomRequestRepository(db)
            runBlocking { forms.saveDraft(FormDraft("local", "Pedido local", listOf(
                FormFieldDraft("name", FormFieldType.TEXT, "Nombre", required = true)), 1, 1)) }
        }
        override fun after() { db.close() }
    }
    private val compose = createComposeRule()
    @get:Rule val rules = RuleChain.outerRule(databaseRule).around(compose)

    @Test fun normalAppDisablesDraftResponsesAndPreviewNeverCreatesRequests() {
        compose.setContent { SpanishTestTheme { FormLinkRoute(forms, SampleTemplateRepository(), requests) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Responder").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Responder").assertIsNotEnabled()
        compose.onNodeWithText("Guardar solicitud en este dispositivo").assertDoesNotExist()
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithContentDescription("Vista previa").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Solo una prueba")
        compose.onNodeWithText("Probar envío").performScrollTo().performClick()
        compose.onNodeWithText("Respuesta de prueba completada. No se envió ningún dato.").assertExists()
        runBlocking { assertEquals(0, requests.getRequests().size) }
    }

    @Test fun fillSaveViewChangeStatusAndReturnShowsUpdatedCounts() {
        // Enter capture directly as a test fixture, never through a draft in app navigation.
        compose.setContent {
            var showApp by remember { mutableStateOf(false) }
            SpanishTestTheme {
                if (showApp) FormLinkRoute(forms, SampleTemplateRepository(), requests)
                else SubmitRequestRoute("local", "fixture-response", forms, requests,
                    onBack = { showApp = true }, onViewRequest = { showApp = true })
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Guardar solicitud en este dispositivo").performScrollTo().performClick()
        compose.onNodeWithText("Este campo es obligatorio").assertExists()
        runBlocking { assertEquals(0, requests.getRequests().size) }
        compose.onNode(hasSetTextAction()).performTextInput("Ana local")
        compose.onNodeWithText("Guardar solicitud en este dispositivo").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ver solicitud guardada").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ver solicitud guardada").performClick()
        compose.onNodeWithText("Respuestas").performClick()
        compose.onNodeWithTag("request-local/fixture-response").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Completada").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Completada").performClick()
        compose.waitUntil(10_000) { runBlocking { requests.getRequests().single().requestStatus == RequestStatus.COMPLETED } }
        compose.onNodeWithText("Ana local").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Completada: 1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Total: 1").assertExists()
        compose.onNodeWithText("Pendiente: 0").assertExists()
        compose.onNodeWithText("Formularios").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("1 respuesta", substring = true).fetchSemanticsNodes().isNotEmpty() }
        runBlocking { assertEquals(1, requests.getRequests().size) }
    }
}
