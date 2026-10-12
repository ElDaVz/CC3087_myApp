package com.uvg.cc3087.myapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.FormPreviewScreen
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import org.junit.Rule
import org.junit.Test

class EditorPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun previewValidatesAnswersAndReturnsToUnchangedEditor() {
        compose.setContent {
            MyappTheme {
                EditForm("Encuesta", listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre", required = true)), {})
            }
        }
        compose.onNodeWithText("Vista previa").performClick()
        compose.onNodeWithText("Probar envío").performScrollTo().performClick()
        compose.onNodeWithText("Este campo es obligatorio").assertExists()
        compose.onNodeWithText("Tu respuesta").performTextInput("Ana")
        compose.onNodeWithText("Probar envío").performScrollTo().performClick()
        compose.onNodeWithText("Respuesta de prueba completada. No se envió ningún dato.").assertExists()
        compose.onNodeWithContentDescription("Volver al editor").performClick()
        compose.onNodeWithText("Nombre").assertExists()
        compose.onNodeWithText("Ana").assertDoesNotExist()
        compose.onNodeWithText("Vista previa").performClick()
        pressBack()
        compose.onNodeWithText("Editar formulario").assertExists()
    }

    @Test fun moveButtonsChangePreviewOrder() {
        compose.setContent {
            MyappTheme {
                EditForm("Encuesta", listOf(
                    FormFieldDraft("a", FormFieldType.TEXT, "Primero"),
                    FormFieldDraft("b", FormFieldType.TEXT, "Segundo")
                ), {})
            }
        }
        compose.onNodeWithContentDescription("Mover campo 1 arriba").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Mover campo 1 abajo").performClick()
        compose.onNodeWithText("Vista previa").performClick()
        compose.onNodeWithText("1. Segundo").assertExists()
        compose.onNodeWithText("2. Primero").assertExists()
    }

    @Test fun previewAnswersSurviveRestorationAndResetClearsThem() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MyappTheme {
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
            MyappTheme {
                EditForm("Encuesta", listOf(FormFieldDraft(
                    "choice", FormFieldType.MULTIPLE_CHOICE, "Preferencias",
                    options = listOf(FieldOption("a", "Café"), FieldOption("b", "Té"))
                )), {})
            }
        }
        compose.onNodeWithText("Varias").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Vista previa").performClick()
        compose.onNodeWithText("Selecciona todas las opciones que correspondan").assertExists()
        compose.onNodeWithText("Café").performClick()
        compose.onNodeWithText("Té").performClick()
        compose.onNodeWithText("Café").assertIsOn()
        compose.onNodeWithText("Té").assertIsOn()
    }
}
