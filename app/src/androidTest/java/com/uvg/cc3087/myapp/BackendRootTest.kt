package com.uvg.cc3087.myapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.cc3087.myapp.ui.navigation.FormLinkApp
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.first

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BackendRootTest {
    // Sync emits from worker threads. Queue recomposition instead of resuming it unconfined there.
    @get:Rule val compose = createComposeRule(effectContext = kotlinx.coroutines.test.StandardTestDispatcher())

    @org.junit.Before @org.junit.After fun resetEmulatorSession() = kotlinx.coroutines.runBlocking {
        if (BuildConfig.FIREBASE_EMULATORS) {
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            com.uvg.cc3087.myapp.di.AppContainer.initialize(context)
            val services = com.uvg.cc3087.myapp.di.AppContainer.backend
            val state = services.auth.state.first { it !is com.uvg.cc3087.myapp.data.model.AuthState.Initializing }
            (state as? com.uvg.cc3087.myapp.data.model.AuthState.SignedIn)?.session?.userId?.let(services::cancel)
            services.auth.signOut()
        }
    }
    @Test fun realSignupUsesWorkManagerAndSignOutClearsTheAccountEditor() {
        org.junit.Assume.assumeTrue(BuildConfig.FIREBASE_EMULATORS &&
            androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val email = "ui-${java.util.UUID.randomUUID()}@example.com"
        val title = "Pedido UI ${java.util.UUID.randomUUID()}"
        compose.setContent { SpanishTestTheme { FormLinkApp() } }
        compose.onNodeWithContentDescription("Cuenta").performClick()
        compose.onNodeWithText("Crear una cuenta nueva").performScrollTo().performClick()
        compose.onNodeWithText("Correo electrónico").performTextInput(email)
        compose.onNodeWithText("Contraseña").performTextInput("password123")
        compose.onNodeWithText("Confirmar contraseña").performTextInput("password123")
        compose.onNode(hasText("Crear cuenta") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Mis formularios").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario de pedido").performClick()
        compose.onNode(hasSetTextAction() and hasText("Formulario de pedido")).performTextReplacement(title)
        compose.onNodeWithTag("editor-list").performScrollToNode(hasText("Continuar"))
        compose.onNodeWithText("Continuar").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¡Formulario guardado!").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Volver a mis formularios").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Sincronizado con tu cuenta").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Cuenta").performClick()
        compose.onNodeWithText("Correo electrónico").assertDoesNotExist()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").assertDoesNotExist()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Mis formularios").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.onNodeWithContentDescription("Cuenta").performClick()
        compose.onNodeWithText("Correo electrónico").performTextInput(email)
        compose.onNodeWithText("Contraseña").performTextInput("password123")
        compose.onNode(hasText("Iniciar sesión") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Cuenta").performClick()
        compose.onNodeWithText("Correo electrónico").assertDoesNotExist()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").assertDoesNotExist()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Mis formularios").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun guestEditorSurvivesSavedStateRestorationWithItsSessionOwner() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        val title = "Restaurar ${java.util.UUID.randomUUID()}"
        restoration.setContent { SpanishTestTheme { FormLinkApp() } }
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario en blanco").performClick()
        compose.onNode(hasSetTextAction() and hasText("Formulario sin título")).performTextReplacement(title)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Guardado en este dispositivo").fetchSemanticsNodes().isNotEmpty() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction() and hasText(title)).assertExists()
        compose.onNodeWithText("Editar formulario").assertExists()
    }

    @Test fun guestCanUseTheAppAndOpenTheAccountScreenWithScopedViewModels() {
        compose.setContent { SpanishTestTheme { FormLinkApp() } }
        compose.onNodeWithText("Mis formularios").assertExists()
        compose.onNodeWithContentDescription("Cuenta").performClick()
        compose.onNodeWithText("Correo electrónico").assertExists()
        compose.onNodeWithText("Cerrar sesión").assertDoesNotExist()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").performScrollTo().performClick()
        compose.onNodeWithText("Enviar enlace").assertExists()
        compose.onNodeWithText("Contraseña").assertDoesNotExist()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Continuar como invitado").performScrollTo().performClick()
        compose.onNodeWithText("Mis formularios").assertExists()
    }
}
