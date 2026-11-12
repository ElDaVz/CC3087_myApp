package com.uvg.cc3087.myapp

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.cc3087.myapp.ui.screens.AuthScreen
import com.uvg.cc3087.myapp.ui.viewmodel.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class AuthScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recoveryShowsOnlyConfirmationAndBackAfterAcceptedRequest() {
        var state by mutableStateOf(AuthUiState(email = "ana@example.com"))
        var backs = 0
        compose.setContent { SpanishTestTheme {
            AuthScreen(state, true, {}, {}, {}, { state = state.copy(mode = it) },
                { state = state.copy(recoverySent = true) }, { backs++ })
        } }
        compose.onNodeWithText("¿Olvidaste tu contraseña?").performScrollTo().performClick()
        compose.onNodeWithText("ana@example.com").assertExists()
        compose.onNodeWithText("Contraseña").assertDoesNotExist()
        compose.onNodeWithText("Enviar enlace").performScrollTo().performClick()
        compose.onNodeWithText("Revisa tu email").assertIsDisplayed()
        compose.onNodeWithText("Correo electrónico").assertDoesNotExist()
        compose.onNodeWithText("Enviar enlace").assertDoesNotExist()
        compose.onNodeWithText("Volver").performClick()
        assertEquals(1, backs)
    }

    @Test fun loadingDisablesFieldsSubmissionNavigationAndModeChanges() {
        var calls = 0
        compose.setContent { SpanishTestTheme {
            AuthScreen(AuthUiState(busy = true), true, { calls++ }, { calls++ }, { calls++ },
                { calls++ }, { calls++ }, { calls++ })
        } }
        compose.onNodeWithText("Correo electrónico").assertIsNotEnabled()
        compose.onNodeWithText("Contraseña").assertIsNotEnabled()
        compose.onNode(hasText("Iniciar sesión") and hasClickAction()).assertIsNotEnabled()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").assertIsNotEnabled()
        compose.onNodeWithText("Crear una cuenta nueva").assertIsNotEnabled()
        compose.onNodeWithText("Volver").assertIsNotEnabled()
        compose.onNodeWithText("Continuar como invitado").assertIsNotEnabled()
        assertEquals(0, calls)
    }
}
