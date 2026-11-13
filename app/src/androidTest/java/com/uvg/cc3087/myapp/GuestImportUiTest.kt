package com.uvg.cc3087.myapp

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.cc3087.myapp.data.model.UserSession
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.ui.components.GuestImportNotice
import com.uvg.cc3087.myapp.ui.screens.AccountScreen
import com.uvg.cc3087.myapp.ui.viewmodel.AccountUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GuestImportUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun retryShowsProgressThenLocalResultWithoutClaimingCloudSync() {
        val state = mutableStateOf(GuestImportState(GuestImportPhase.ERROR, 2))
        var retries = 0
        compose.setContent { SpanishTestTheme {
            GuestImportNotice(state.value) { retries++; state.value = GuestImportState(GuestImportPhase.IMPORTING, 2) }
        } }
        compose.onNodeWithText("Reintentar importación").performClick()
        compose.onNodeWithText("Incorporando tus borradores de invitado a esta cuenta…").assertExists()
        compose.onNodeWithText("Reintentar importación").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, retries); state.value = GuestImportState(GuestImportPhase.COMPLETE, 2) }
        compose.onNodeWithText("Sincronizado con tu cuenta").assertDoesNotExist()
        compose.onNodeWithText("2 borradores de invitado incorporados", substring = true).assertExists()
    }

    @Test fun accountKeepsNavigationAndSignOutAndHasNoManualSelectionStep() {
        var backs = 0
        var signOuts = 0
        compose.setContent { SpanishTestTheme {
            AccountScreen(UserSession("a", "example@example.com"), AccountUiState(),
                GuestImportState(GuestImportPhase.IMPORTING, 2), null,
                onBack = { backs++ }, onRetryImport = {}, onSignOut = { signOuts++ }, onSync = {})
        } }
        compose.onNodeWithText("Importar borradores seleccionados").assertDoesNotExist()
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, backs); assertEquals(1, signOuts) }
    }
}
