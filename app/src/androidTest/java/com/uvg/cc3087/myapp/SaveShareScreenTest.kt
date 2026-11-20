package com.uvg.cc3087.myapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.repository.FormLinks
import com.uvg.cc3087.myapp.data.repository.FormPublication
import com.uvg.cc3087.myapp.data.repository.InvitationMode
import com.uvg.cc3087.myapp.ui.screens.SaveShareScreen
import com.uvg.cc3087.myapp.ui.state.SaveShareUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SaveShareScreenTest {
    @get:Rule val compose = createComposeRule()
    private val draft = FormDraft(
        "saved", "Pedido", listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre")), 100, 200
    )

    @Test fun aLocalDraftShowsItsRealStatusAndDisablesPublicSharing() {
        var backToForms = 0
        compose.setContent {
            SpanishTestTheme {
                SaveShareScreen(
                    SaveShareUiState(draft = draft), {}, { backToForms++ }, {},
                    onCopyLink = { error("A draft has no public link") },
                    onWhatsAppShare = { error("A draft must not be shared as published") }
                )
            }
        }
        compose.onNodeWithText("¡Formulario guardado!").assertExists()
        compose.onNodeWithText("Borrador").assertExists()
        compose.onNodeWithText("Todavía no hay enlace público").assertExists()
        compose.onNodeWithText("Compartir por WhatsApp").assertIsNotEnabled()
        compose.onNodeWithText("Copiar enlace").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Copiar enlace").assertIsNotEnabled()
        compose.onNodeWithText("Volver a mis formularios").performScrollTo().performClick()
        assertEquals(1, backToForms)
    }

    @Test fun aProvidedPublicLinkUsesTheSameValueForCopyAndWhatsApp() {
        // este enlace es solo un dato de prueba, nunca lo genera el repositorio local
        val link = "https://example.invalid/forms/test"
        var copied: String? = null
        var shared: String? = null
        compose.setContent {
            SpanishTestTheme {
                SaveShareScreen(
                    SaveShareUiState(draft = draft, publicLink = link), {}, {}, {},
                    onCopyLink = { copied = it; true },
                    onWhatsAppShare = { shared = it; true }
                )
            }
        }
        compose.onNodeWithText("Copiar enlace").performScrollTo().performClick()
        compose.onNodeWithText("Compartir por WhatsApp").performScrollTo().performClick()
        assertEquals(link, copied)
        assertEquals(link, shared)
    }

    @Test fun singleUseInvitationChangesTheCopiedLinkAndTrackingUsesSystemShare() {
        val publication = FormPublication(
            "https://example.invalid/f/saved", "PUBLISHED", 1, 100, true, null,
            FormLinks(
                "https://example.invalid/f/saved",
                "https://example.invalid/i/abcdefghijklmnopqrstuvwx",
                "trackingtoken0123456789"
            )
        )
        var copied: String? = null
        var systemShared: String? = null
        var mode: InvitationMode? = null
        compose.setContent {
            SpanishTestTheme {
                SaveShareScreen(
                    SaveShareUiState(draft = draft, publicLink = publication.url, publication = publication),
                    {}, {}, {},
                    onCopyLink = { copied = it; true },
                    onWhatsAppShare = { true },
                    onSystemShare = { systemShared = it; true },
                    onInvitationMode = { mode = it }
                )
            }
        }
        compose.onNodeWithTag("invitation-mode-SINGLE_USE").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(InvitationMode.SINGLE_USE, mode) }
        compose.onNodeWithText("Copiar enlace").performScrollTo().performClick()
        assertEquals("https://example.invalid/i/abcdefghijklmnopqrstuvwx", copied)
        compose.onNodeWithText("Compartir enlace de seguimiento").performScrollTo().performClick()
        assertEquals("https://example.invalid/t/trackingtoken0123456789", systemShared)
    }
}
