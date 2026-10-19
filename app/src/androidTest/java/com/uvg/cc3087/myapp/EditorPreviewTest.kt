package com.uvg.cc3087.myapp

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.ui.viewmodel.EditFormViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.flowOf
import java.io.Serializable
import java.util.Locale
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.FormPreviewScreen
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import org.junit.Rule
import org.junit.Test

class EditorPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun previewValidatesAnswersAndReturnsToUnchangedEditor() {
        compose.setContent {
            SpanishTestTheme {
                TestEditor("Encuesta", listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre", required = true)))
            }
        }
        compose.onNodeWithText("Vista previa").assertDoesNotExist()
        compose.onNodeWithContentDescription("Vista previa").assertHasClickAction().performClick()
        compose.onNodeWithText("Probar envío").performScrollTo().performClick()
        compose.onNodeWithText("Este campo es obligatorio").assertExists()
        compose.onNodeWithText("Tu respuesta").performTextInput("Ana")
        compose.onNodeWithText("Probar envío").performScrollTo().performClick()
        compose.onNodeWithText("Respuesta de prueba completada. No se envió ningún dato.").assertExists()
        compose.onNodeWithContentDescription("Volver al editor").performClick()
        compose.onNodeWithText("Nombre").assertExists()
        compose.onNodeWithText("Ana").assertDoesNotExist()
        compose.onNodeWithContentDescription("Vista previa").performClick()
        pressBack()
        compose.onNodeWithText("Editar formulario").assertExists()
    }

    @Test fun moveButtonsChangePreviewOrder() {
        compose.setContent {
            SpanishTestTheme {
                TestEditor("Encuesta", listOf(
                    FormFieldDraft("a", FormFieldType.TEXT, "Primero"),
                    FormFieldDraft("b", FormFieldType.TEXT, "Segundo")
                ))
            }
        }
        compose.onNodeWithContentDescription("Mover campo 1 arriba").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Mover campo 1 abajo").performClick()
        compose.onNodeWithContentDescription("Vista previa").performClick()
        compose.onNodeWithText("1. Segundo").assertExists()
        compose.onNodeWithText("2. Primero").assertExists()
    }

    @Test fun previewAnswersSurviveRestorationAndResetClearsThem() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SpanishTestTheme {
                FormPreviewScreen("Encuesta", listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre")), {})
            }
        }
        compose.onNodeWithText("Tu respuesta").performTextInput("Ana")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Ana").assertExists()
        compose.onNodeWithText("Reiniciar").performClick()
        compose.onNodeWithText("Ana").assertDoesNotExist()
    }

    @Test fun selectionModeAndCustomOptionsSurviveEditorRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SpanishTestTheme {
                TestEditor("Encuesta", listOf(FormFieldDraft(
                    "choice", FormFieldType.MULTIPLE_CHOICE, "Preferencias",
                    options = listOf(FieldOption("a", "Café"), FieldOption("b", "Té"))
                )))
            }
        }
        compose.onNodeWithText("Varias").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Vista previa").performClick()
        compose.onNodeWithText("Selecciona todas las opciones que correspondan").assertExists()
        compose.onNodeWithText("Café").performClick()
        compose.onNodeWithText("Té").performClick()
        compose.onNodeWithText("Café").assertIsOn()
        compose.onNodeWithText("Té").assertIsOn()
    }
}

// mantenemos el idioma de las pruebas sin cambiar la configuración del emulador
@Composable
private fun SpanishTestTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = remember(context) {
        Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("es")) }
    }
    val resources = remember(context, configuration) {
        context.createConfigurationContext(configuration).resources
    }
    CompositionLocalProvider(
        LocalResources provides resources,
        LocalConfiguration provides configuration
    ) {
        MyappTheme(content = content)
    }
}

// el mismo viewmodel de producción recibe un repositorio pequeño para estas pruebas
@Composable
private fun TestEditor(title: String, initialFields: List<FormFieldDraft>) {
    val handleSaver = remember {
        Saver<SavedStateHandle, Serializable>(
            save = { handle -> HashMap(handle.keys().associateWith { handle.get<Any?>(it) }) },
            restore = {
                @Suppress("UNCHECKED_CAST")
                SavedStateHandle(it as Map<String, Any?>)
            }
        )
    }
    val saved = rememberSaveable(saver = handleSaver) { SavedStateHandle() }
    val viewModel = remember(saved) {
        val repository = object : TemplateRepository {
            override fun observeTemplates() = flowOf(emptyList<FormTemplate>())
            override fun getInitialFields(templateType: TemplateType) = initialFields
        }
        EditFormViewModel(repository, saved).apply {
            if (!saved.contains("editor_title")) startDraft(title, TemplateType.ORDER_FORM)
        }
    }
    val state by viewModel.uiState.collectAsState()
    EditForm(state, viewModel::onAction, viewModel::validate, onBackClick = {})
}
