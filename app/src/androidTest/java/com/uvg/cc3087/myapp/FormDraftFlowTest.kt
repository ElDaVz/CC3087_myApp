package com.uvg.cc3087.myapp

import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.OfflineFirstFormRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class FormDraftFlowTest {
    private lateinit var database: FormLinkDatabase
    private lateinit var repository: OfflineFirstFormRepository
    private val databaseRule = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            database = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
            repository = database.guestFormRepository()
        }

        override fun after() { database.close() }
    }
    private val compose = createAndroidComposeRule<ComponentActivity>()
    // cerramos la base después de que la actividad de prueba haya liberado sus viewmodels
    @get:Rule val rules = RuleChain.outerRule(databaseRule).around(compose)

    @Test fun deletionCanBeCancelledAndConfirmedFromTheList() {
        runBlocking { repository.saveDraft(FormDraft("stored", "Pedido guardado", emptyList(), 1, 1)) }
        launchApp(repository)
        waitForNode(hasContentDescription("Eliminar formulario Pedido guardado"))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("¿Eliminar formulario?").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        waitForForm("Pedido guardado")
        runBlocking { assertEquals(1, repository.observeForms().first().size) }
        waitForNode(hasContentDescription("Eliminar formulario Pedido guardado"))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Eliminar", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Pedido guardado").fetchSemanticsNodes().isEmpty()
        }
        runBlocking { assertEquals(0, repository.observeForms().first().size) }
    }

    @Test fun continueValidatesThenOpensTheSavedFormWithoutPreview() {
        launchApp(repository)
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario en blanco").performClick()
        compose.onNodeWithTag("editor-list").performScrollToNode(hasText("Continuar"))
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("¡Formulario guardado!").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Formulario de pedido").performClick()
        compose.onNodeWithTag("editor-list").performScrollToNode(hasText("Continuar"))
        compose.onNodeWithText("Continuar").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¡Formulario guardado!").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Copiar enlace").assertIsNotEnabled()
    }

    @Test fun createEditAndReopenFromFormsKeepsTheSameDraft() {
        launchApp(repository)
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario de pedido").performClick()
        waitForNode(hasSetTextAction() and hasText("Formulario de pedido"))
            .performTextReplacement("Pedido persistente")
        waitForNode(hasSetTextAction() and hasText("Nombre"))
            .performTextReplacement("Nombre del cliente")
        waitForSaved()
        compose.onNodeWithContentDescription("Volver").performClick()
        dispatchBack()
        waitForForm("Pedido persistente")
        openEditor()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasSetTextAction() and hasText("Nombre del cliente"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        waitForNode(hasSetTextAction() and hasText("Pedido persistente"))
            .performTextReplacement("Pedido actualizado")
        waitForSaved()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Mis formularios").assertExists()
        waitForForm("Pedido actualizado")

        runBlocking {
            val forms = repository.observeForms().first()
            assertEquals(1, forms.size)
            val draft = repository.getDraft(forms.single().id)!!
            assertEquals("Pedido actualizado", draft.title)
            assertEquals("Nombre del cliente", draft.fields.single().title)
        }
    }

    @Test fun aSaveErrorIsVisibleAndRetryKeepsTheTypedTitle() {
        val failSaves = AtomicBoolean(true)
        val failingRepository = object : FormRepository by repository {
            override suspend fun saveDraft(draft: FormDraft) {
                if (failSaves.get()) throw IOException("test write failure")
                repository.saveDraft(draft)
            }
        }
        launchApp(failingRepository)
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario en blanco").performClick()
        waitForNode(hasSetTextAction() and hasText("Formulario sin título"))
            .performTextReplacement("No perder este título")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("No se pudieron guardar los cambios en este dispositivo")
                .fetchSemanticsNodes().isNotEmpty()
        }
        failSaves.set(false)
        compose.onNodeWithText("Reintentar").performClick()
        waitForSaved()
        waitForNode(hasSetTextAction() and hasText("No perder este título")).assertExists()
        runBlocking {
            assertEquals("No perder este título", repository.observeForms().first().single().title)
        }
    }

    @Test fun previewSaveOpensTheStoredReceiptAndCanReturnToForms() {
        launchApp(repository)
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario de pedido").performClick()
        waitForSaved()
        compose.onNodeWithContentDescription("Vista previa").performClick()
        compose.onNodeWithText("Guardar formulario").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("¡Formulario guardado!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Borrador").assertExists()
        compose.onNodeWithText("Compartir por WhatsApp").assertIsNotEnabled()
        compose.onNodeWithText("Volver a mis formularios").performScrollTo().performClick()
        compose.onNodeWithText("Mis formularios").assertExists()
        waitForNode(hasText("Compartir"))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("¡Formulario guardado!").fetchSemanticsNodes().isNotEmpty()
        }
        dispatchBack()
        compose.onNodeWithText("Mis formularios").assertExists()
        runBlocking { assertEquals(1, repository.observeForms().first().size) }
    }

    @Test fun aFilterWithoutMatchesShowsAnEmptyMessageAndCanReturnToAll() {
        runBlocking {
            repository.saveDraft(FormDraft("stored", "Pedido guardado", emptyList(), 1, 1))
        }
        val releaseForms = CompletableDeferred<Unit>()
        val delayedRepository = object : FormRepository by repository {
            override fun observeForms() = flow {
                releaseForms.await()
                emitAll(repository.observeForms())
            }
        }
        launchApp(delayedRepository)
        waitForNode(hasText("Cargando tus formularios…")).assertIsDisplayed()
        compose.onNodeWithText("Pedido guardado").assertDoesNotExist()
        releaseForms.complete(Unit)
        waitForForm("Pedido guardado")
        repeat(3) {
            compose.onNodeWithText("Activos").performClick()
            waitForNode(hasText("No hay formularios que coincidan con este filtro")).assertIsDisplayed()
            compose.onNodeWithText("Activos").assertIsSelected()
            compose.onNodeWithText("Pedido guardado").assertDoesNotExist()
            compose.onNodeWithText("Todos").performClick()
            waitForForm("Pedido guardado")
            compose.onNodeWithText("Todos").assertIsSelected()
            compose.onNodeWithText("No hay formularios que coincidan con este filtro").assertDoesNotExist()
        }
    }

    @Test fun aFailedEditSurvivesReturningToFormsAndReopening() {
        runBlocking {
            repository.saveDraft(FormDraft("stored", "Pedido guardado", emptyList(), 1, 1))
        }
        val failSaves = AtomicBoolean(true)
        val failingRepository = object : FormRepository by repository {
            override suspend fun saveDraft(draft: FormDraft) {
                if (failSaves.get()) throw IOException("test write failure")
                repository.saveDraft(draft)
            }
        }
        launchApp(failingRepository)
        openEditor()
        waitForNode(hasSetTextAction() and hasText("Pedido guardado"))
            .performTextReplacement("Edición pendiente")
        waitForNode(hasText("No se pudieron guardar los cambios en este dispositivo")).assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        waitForForm("Pedido guardado")
        openEditor()
        waitForNode(hasSetTextAction() and hasText("Edición pendiente")).assertExists()
        failSaves.set(false)
        compose.onNodeWithText("Reintentar").performClick()
        waitForSaved()
        runBlocking { assertEquals("Edición pendiente", repository.getDraft("stored")!!.title) }
    }

    private fun launchApp(forms: FormRepository) {
        val templates = object : TemplateRepository {
            override fun observeTemplates() = flowOf(TemplateSampleData.templates)
            override fun getInitialFields(templateType: TemplateType) =
                listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre"))
        }
        compose.setContent { SpanishTestTheme { FormLinkRoute(forms, templates) } }
    }

    private fun openEditor() {
        waitForNode(hasText("Editar")).performScrollTo().assertIsDisplayed().performClick()
    }

    private fun waitForForm(title: String) {
        waitForNode(hasText("Mis formularios")).assertIsDisplayed()
        waitForNode(hasText(title)).performScrollTo().assertIsDisplayed()
    }

    private fun waitForNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        // Compose idleness does not wait for Room queries or lifecycle Flow emissions.
        // Use the unmerged tree for labels inside clickable cards and buttons.
        compose.waitUntil(10_000) {
            compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
        return compose.onNode(matcher, useUnmergedTree = true)
    }

    private fun dispatchBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun waitForSaved() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Guardado en este dispositivo").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
