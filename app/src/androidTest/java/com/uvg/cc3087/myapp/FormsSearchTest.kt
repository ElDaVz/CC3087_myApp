package com.uvg.cc3087.myapp

import android.os.ParcelFileDescriptor
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FormsSearchTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val forms = SampleFormRepository().apply {
        updateSampleForms(listOf(
            FormSummary("active", "Inscripción", 0, "", FormStatus.ACTIVE, ""),
            FormSummary("order", "Pedido", 0, "", FormStatus.DRAFT, ""),
            FormSummary("monthly", "Pedido mensual", 0, "", FormStatus.DRAFT, "")
        ))
    }

    @Test fun searchFiltersClearsResetsAndClosesWithoutDiscardingTheFilter() {
        launchApp()
        compose.onNodeWithContentDescription("Buscar formularios").performClick()
        search().assertIsFocused().performTextInput("pediod")
        search().performImeAction()
        waitForTitle("Pedido")
        compose.onNodeWithText("Inscripción", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Borradores").performClick()
        compose.onNodeWithContentDescription("Limpiar búsqueda").performClick()
        search().assertTextContains("")
        compose.onNodeWithText("Borradores").assertIsSelected()
        search().performTextReplacement("inexistente")
        search().performImeAction()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("No hay formularios que coincidan con tu búsqueda").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Mostrar todos los formularios").performScrollTo().performClick()
        compose.onNodeWithText("Todos").assertIsSelected()
        waitForTitle("Inscripción")
        compose.onNodeWithText("Borradores").performClick()
        compose.onNodeWithContentDescription("Cerrar búsqueda").performClick()
        search().assertDoesNotExist()
        compose.onNodeWithText("Borradores").assertIsSelected()
        compose.onNodeWithContentDescription("Buscar formularios").performClick()
        search().assertTextContains("")
    }

    @Test fun backDismissesTheKeyboardBeforeClosingSearch() {
        // AVDs expose a hardware keyboard. Exercise the real software IME explicitly,
        // then restore the device preference even when an assertion fails.
        val previous = shell("settings get secure show_ime_with_hard_keyboard").trim()
        try {
            shell("settings put secure show_ime_with_hard_keyboard 1")
            launchApp()
            compose.onNodeWithContentDescription("Buscar formularios").performClick()
            search().performClick().performTextInput("pedido")
            waitForIme(true)
            deviceBack()
            waitForIme(false)
            search().assertExists().assertTextContains("pedido")
            deviceBack()
            compose.waitUntil { compose.onAllNodesWithTag("forms-search").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Mis formularios").assertExists()
            compose.onNodeWithContentDescription("Buscar formularios").performClick()
            search().assertTextContains("")
        } finally {
            if (previous == "null") shell("settings delete secure show_ime_with_hard_keyboard")
            else shell("settings put secure show_ime_with_hard_keyboard $previous")
        }
    }

    @Test fun returningFromEditorAndRestoringPreserveSearchFilterAndScroll() {
        forms.updateSampleForms(emptyList())
        runBlocking {
            repeat(20) { index ->
                forms.saveDraft(FormDraft("order-$index", "Pedido $index", emptyList(), 1, 20L - index))
            }
        }
        val restoration = launchApp()
        compose.onNodeWithContentDescription("Buscar formularios").performClick()
        search().performTextInput("pedido")
        search().performImeAction()
        compose.onNodeWithText("Borradores").performClick()
        compose.onNodeWithTag("forms-list").performScrollToNode(hasText("Pedido 12"))
        compose.onNodeWithText("Pedido 12", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Editar formulario").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitForTitle("Pedido 12")
        search().assertTextContains("pedido")
        compose.onNodeWithText("Pedido 12", useUnmergedTree = true).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        search().assertTextContains("pedido")
        compose.onNodeWithText("Pedido 12", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("forms-list").performScrollToIndex(0)
        compose.onNodeWithText("Borradores").assertIsSelected()
        assertEquals(false, imeVisible())
    }

    private fun search() = compose.onNodeWithTag("forms-search")
    private fun launchApp() = StateRestorationTester(compose).also { restoration ->
        restoration.setContent {
            SpanishTestTheme { FormLinkRoute(forms, SampleTemplateRepository()) }
        }
    }
    private fun waitForTitle(title: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(title, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    private fun imeVisible() = compose.runOnIdle {
        compose.activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
    }
    private fun waitForIme(visible: Boolean) = compose.waitUntil(10_000) { imeVisible() == visible }
    private fun deviceBack() {
        shell("input keyevent KEYCODE_BACK")
        compose.waitForIdle()
    }
    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
