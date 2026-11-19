package com.uvg.cc3087.myapp

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TypedNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val forms = SampleFormRepository().apply { updateSampleForms(emptyList()) }
    private val templates = object : TemplateRepository {
        override fun observeTemplates() = flowOf(TemplateSampleData.templates)
        override fun getInitialFields(templateType: TemplateType) =
            listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre"))
    }

    @Test fun previewRestoresItsAnswersAndBackReturnsToTheSameEditor() {
        val restoration = launchApp()
        openOrderEditor()
        compose.onNodeWithContentDescription("Vista previa").performClick()
        compose.onNodeWithText("Tu respuesta").performTextInput("Respuesta de prueba")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Respuesta de prueba").assertExists()
        pressBack()
        compose.onNodeWithText("Editar formulario").assertExists()
        compose.onNode(hasSetTextAction() and hasText("Formulario de pedido")).assertExists()
        compose.onNodeWithText("Respuesta de prueba").assertDoesNotExist()
        runBlocking { assertEquals(1, forms.observeForms().first().size) }
    }

    @Test fun restoredReceiptKeepsItsDraftAndReturnsToTheEditorNotPreview() {
        val restoration = launchApp()
        openOrderEditor()
        compose.onNodeWithContentDescription("Vista previa").performClick()
        compose.onNodeWithText("Guardar formulario").performScrollTo().performClick()
        compose.onNodeWithText("¡Formulario guardado!").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("¡Formulario guardado!").assertExists()
        compose.onNodeWithText("Borrador").assertExists()
        pressBack()
        compose.onNodeWithText("Editar formulario").assertExists()
        compose.onNodeWithContentDescription("Vista previa").assertExists()
        compose.onNode(hasSetTextAction() and hasText("Formulario de pedido")).assertExists()
        runBlocking {
            val stored = forms.observeForms().first()
            assertEquals(1, stored.size)
            assertEquals("Formulario de pedido", forms.getDraft(stored.single().id)!!.title)
        }
    }

    @Test fun accountIsARestorableDestinationAndDoesNotResetTheFormsFilter() {
        val restoration = launchApp()
        compose.onNodeWithText("Activos").performClick()
        compose.onNodeWithContentDescription("Cuenta").performClick()
        compose.onNodeWithText("Cuenta de prueba").assertExists()
        compose.onNodeWithText("Mis formularios").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Cuenta de prueba").assertExists()
        pressBack()
        compose.onNodeWithText("Mis formularios").assertExists()
        compose.onNodeWithText("Activos").assertIsSelected()
        compose.onNodeWithContentDescription("Cuenta").performClick()
        compose.onNodeWithText("Regresar").performClick()
        compose.onNodeWithText("Activos").assertIsSelected()
    }

    private fun launchApp(): StateRestorationTester = StateRestorationTester(compose).also {
        it.setContent {
            SpanishTestTheme {
                FormLinkRoute(forms, templates, accountContent = { onBack ->
                    Button(onClick = onBack) { Text("Regresar") }
                    Text("Cuenta de prueba")
                })
            }
        }
    }

    private fun openOrderEditor() {
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario de pedido").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Guardado en este dispositivo").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pressBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
}
