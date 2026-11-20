package com.uvg.cc3087.myapp

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.model.OwnerProfile
import com.uvg.cc3087.myapp.data.model.UserSession
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.ui.screens.AccountScreen
import com.uvg.cc3087.myapp.ui.viewmodel.AccountUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AccountScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun statesActionsAndAccessibleBack() {
        val sync = mutableStateOf(AccountSyncState())
        var checks = 0
        var backs = 0
        compose.setContent { SpanishTestTheme {
            AccountScreen(UserSession("a", "account@example.com"), AccountUiState(), GuestImportState(), sync.value,
                { backs++ }, {}, {}, { checks++ })
        } }
        compose.onNodeWithText("Sin comprobar").assertExists()
        compose.onNodeWithText("Comprobar ahora").performScrollTo().performClick()
        compose.runOnIdle { sync.value = AccountSyncState(AccountSyncPhase.WAITING_CONNECTION, AccountPendingChanges(2, 1, 3)) }
        compose.onNodeWithText("Esperando conexión").assertExists()
        compose.onNodeWithText("Sincronizar ahora").assertIsNotEnabled()
        compose.onNodeWithText("Borradores modificados: 2").assertExists()
        compose.onNodeWithText("Eliminaciones pendientes: 1").assertExists()
        compose.onNodeWithText("Cambios de estado de respuestas: 3").assertExists()
        compose.runOnIdle { sync.value = sync.value.copy(phase = AccountSyncPhase.ERROR) }
        compose.onNodeWithText("Sincronizar / reintentar").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.runOnIdle { assertEquals(2, checks); assertEquals(1, backs) }
    }
    @Test fun signOutDialogCanCancelOrContinue() {
        var cancelled = 0
        var confirmed = 0
        compose.setContent { SpanishTestTheme {
            AccountScreen(UserSession("a", "account@example.com"), AccountUiState(confirmSignOut = true), GuestImportState(), AccountSyncState(),
                {}, {}, {}, {}, { confirmed++ }, { cancelled++ })
        } }
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNode(hasText("Cerrar sesión") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(1, cancelled); assertEquals(1, confirmed) }
    }
    @Test fun contactEmailsAcceptPartialTextAndSaveWhenEditingEnds() {
        var profile by mutableStateOf(OwnerProfile("a"))
        val saved = mutableListOf<OwnerProfile>()
        compose.setContent { SpanishTestTheme {
            AccountScreen(UserSession("a", "account@example.com"), AccountUiState(), GuestImportState(), AccountSyncState(),
                {}, {}, {}, {},
                profile = profile,
                onProfileChange = { change ->
                    profile = change(profile)
                    saved += profile
                })
        } }
        val personal = compose.onNodeWithTag("profile-personal-email")
        val business = compose.onNodeWithTag("profile-business-email")

        personal.performScrollTo().performClick()
        personal.performTextInput("ana")
        compose.runOnIdle { assertTrue(saved.isEmpty()) }

        business.performScrollTo().performClick()
        compose.onNodeWithText("Ingresa un correo válido.").assertExists()
        compose.runOnIdle { assertTrue(saved.isEmpty()) }

        personal.performScrollTo().performClick()
        personal.performTextReplacement("ana@example.com")
        business.performScrollTo().performClick()
        compose.runOnIdle { assertEquals("ana@example.com", saved.single().personalEmail) }

        business.performTextInput("team@")
        compose.runOnIdle { assertEquals(1, saved.size) }
        personal.performScrollTo().performClick()
        compose.onNodeWithText("Ingresa un correo válido.").assertExists()
        compose.runOnIdle { assertEquals(1, saved.size) }

        business.performScrollTo().performClick()
        business.performTextReplacement("team@example.com")
        personal.performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("ana@example.com", saved.last().personalEmail)
            assertEquals("team@example.com", saved.last().businessEmail)
        }

        business.performScrollTo().performClick()
        business.performTextClearance()
        personal.performScrollTo().performClick()
        compose.runOnIdle { assertEquals("", saved.last().businessEmail) }
    }

    @Test fun lightDarkAndLargeTextRemainScrollable() {
        val variant = mutableIntStateOf(0)
        compose.setContent { SpanishTestTheme {
            val config = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (variant.intValue == 1) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalConfiguration provides config,
                LocalDensity provides Density(density.density, if (variant.intValue == 2) 1.5f else 1f)) {
                AccountScreen(UserSession("a", "long.account.address@example.com"), AccountUiState(),
                    GuestImportState(GuestImportPhase.COMPLETE, 2),
                    AccountSyncState(AccountSyncPhase.UP_TO_DATE, lastSuccessfulCheck = 1_794_800_000_000), {}, {}, {}, {})
            }
        } }
        for ((index, name) in listOf("light", "dark", "large-text").withIndex()) {
            compose.runOnIdle { variant.intValue = index }
            compose.onNodeWithText("Sesión de FormLink").performScrollTo().assertIsDisplayed()
            capture("account-$name-top")
            compose.onNodeWithText("Cerrar sesión").performScrollTo().assertIsDisplayed()
            capture("account-$name-bottom")
        }
    }

    private fun capture(name: String) {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
