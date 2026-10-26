package com.uvg.cc3087.myapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.RoomFormRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
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
    private lateinit var repository: RoomFormRepository
    private val databaseRule = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            database = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
            repository = RoomFormRepository(database.drafts())
        }

        override fun after() { database.close() }
    }
    private val compose = createComposeRule()
    // cerramos la base después de que la actividad de prueba haya liberado sus viewmodels
    @get:Rule val rules = RuleChain.outerRule(databaseRule).around(compose)

    @Test fun createEditAndReopenFromFormsKeepsTheSameDraft() {
        launchApp(repository)
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario de pedido").performClick()
        compose.onNode(hasSetTextAction() and hasText("Formulario de pedido"))
            .performTextReplacement("Pedido persistente")
        compose.onNode(hasSetTextAction() and hasText("Nombre"))
            .performTextReplacement("Nombre del cliente")
        waitForSaved()
        compose.onNodeWithContentDescription("Volver").performClick()
        pressBack()
        compose.onNodeWithText("Pedido persistente").assertExists()
        compose.onNodeWithText("Editar").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasSetTextAction() and hasText("Nombre del cliente"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction() and hasText("Pedido persistente"))
            .performTextReplacement("Pedido actualizado")
        waitForSaved()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Mis formularios").assertExists()
        compose.onNodeWithText("Pedido actualizado").assertExists()

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
        compose.onNode(hasSetTextAction() and hasText("Formulario sin título"))
            .performTextReplacement("No perder este título")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("No se pudieron guardar los cambios en este dispositivo")
                .fetchSemanticsNodes().isNotEmpty()
        }
        failSaves.set(false)
        compose.onNodeWithText("Reintentar").performClick()
        waitForSaved()
        compose.onNode(hasSetTextAction() and hasText("No perder este título")).assertExists()
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
        compose.onNodeWithText("Compartir").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("¡Formulario guardado!").fetchSemanticsNodes().isNotEmpty()
        }
        pressBack()
        compose.onNodeWithText("Mis formularios").assertExists()
        runBlocking { assertEquals(1, repository.observeForms().first().size) }
    }

    @Test fun aFilterWithoutMatchesShowsAnEmptyMessageAndCanReturnToAll() {
        runBlocking {
            repository.saveDraft(FormDraft("stored", "Pedido guardado", emptyList(), 1, 1))
        }
        launchApp(repository)
        compose.onNodeWithText("Pedido guardado").assertExists()
        compose.onNodeWithText("Activos").performClick()
        compose.onNodeWithText("No hay formularios que coincidan con este filtro").assertExists()
        compose.onNodeWithText("Pedido guardado").assertDoesNotExist()
        compose.onNodeWithText("Todos").performClick()
        compose.onNodeWithText("Pedido guardado").assertExists()
        compose.onNodeWithText("No hay formularios que coincidan con este filtro").assertDoesNotExist()
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
        compose.onNodeWithText("Editar").performClick()
        compose.onNode(hasSetTextAction() and hasText("Pedido guardado"))
            .performTextReplacement("Edición pendiente")
        compose.onNodeWithText("No se pudieron guardar los cambios en este dispositivo").assertExists()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Pedido guardado").assertExists()
        compose.onNodeWithText("Editar").performClick()
        compose.onNode(hasSetTextAction() and hasText("Edición pendiente")).assertExists()
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

    private fun waitForSaved() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Guardado en este dispositivo").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
