package com.uvg.cc3087.myapp

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.ui.navigation.FormLinkRoute
import com.uvg.cc3087.myapp.ui.theme.AppearanceRoot
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val appearance = TestAppearanceRepository()
    private val dark = AtomicBoolean(false)
    private var systemDark by mutableStateOf(false)
    private var owner by mutableStateOf("guest")
    private val forms = SampleFormRepository().apply { updateSampleForms(emptyList()) }
    private val templates = object : TemplateRepository {
        override fun observeTemplates() = kotlinx.coroutines.flow.flowOf(com.uvg.cc3087.myapp.data.TemplateSampleData.templates)
        override fun getInitialFields(templateType: TemplateType) =
            listOf(FormFieldDraft("name", FormFieldType.TEXT, "Nombre"))
    }
    private val requests = object : RequestRepository {
        override suspend fun getRequests() = emptyList<RequestSummary>()
        override suspend fun getDetail(formId: String, responseId: String): RequestDetail? = null
    }

    @Test fun settingsOpensFromEveryBottomBarAndReturnsToItsOrigin() {
        launch()
        openSettings()
        back()
        compose.onNodeWithText("Mis formularios").assertExists()
        compose.onNodeWithText("Nuevo formulario").performClick()
        openSettings()
        back()
        compose.onNodeWithText("Formulario en blanco").assertExists()
        back()
        compose.onNodeWithText("Respuestas").performClick()
        openSettings()
        back()
        compose.onNodeWithTag("response-dashboard").assertExists()
        compose.onNodeWithTag("response-dashboard").performScrollToNode(hasTestTag("metric-total"))
        compose.onNodeWithTag("metric-total").performClick()
        waitForText("Solicitudes recibidas")
        openSettings()
        back()
        compose.onNodeWithText("Solicitudes recibidas").assertExists()
    }

    @Test fun restoredSettingsOpensAccountAndStatisticsAndFormsReturnsToRoot() {
        val restoration = launch()
        compose.onNodeWithText("Activos").performClick()
        openSettings()
        restoration.emulateSavedInstanceStateRestore()
        waitForText("Apariencia")
        compose.onNodeWithText("Cuenta").performScrollTo().performClick()
        waitForText("Cuenta de prueba")
        back()
        waitForText("Apariencia")
        compose.onNodeWithText("Estadísticas").performScrollTo().performClick()
        compose.onNodeWithTag("response-dashboard").assertExists()
        back()
        waitForText("Apariencia")
        compose.onNodeWithText("Estadísticas").performScrollTo().performClick()
        compose.onNodeWithText("Formularios").performClick()
        compose.onNodeWithText("Mis formularios").assertExists()
        compose.onNodeWithText("Activos").assertIsSelected()
    }

    @Test fun appearanceChangesImmediatelyAndSurvivesRestorationAndOwnerChanges() {
        val restoration = launch()
        openSettings()
        select("Oscuro")
        compose.runOnIdle { assertTrue(dark.get()) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Oscuro").assertIsSelected()
        compose.runOnIdle { owner = "account-a" }
        waitForText("Mis formularios")
        openSettings()
        compose.onNodeWithText("Oscuro").assertIsSelected()
        select("Claro")
        compose.runOnIdle { assertFalse(dark.get()); owner = "guest" }
        waitForText("Mis formularios")
        openSettings()
        compose.onNodeWithText("Claro").assertIsSelected()
    }

    @Test fun systemModeFollowsConfigurationButAnExplicitChoiceDoesNot() {
        launch()
        openSettings()
        compose.runOnIdle { assertFalse(dark.get()); systemDark = true }
        compose.waitUntil(10_000) { dark.get() }
        select("Claro")
        compose.runOnIdle { assertFalse(dark.get()); systemDark = false }
        select("Oscuro")
        compose.runOnIdle { assertTrue(dark.get()) }
        select("Sistema")
        compose.runOnIdle { assertFalse(dark.get()); systemDark = true }
        compose.waitUntil(10_000) { dark.get() }
    }

    @Test fun failedWriteRetainsSelectionAndOffersAWorkingRetry() {
        launch()
        openSettings()
        appearance.failWrite = true
        compose.onNodeWithText("Oscuro").performScrollTo().performClick()
        waitForText("No se pudo guardar la apariencia. Se conserva tu selección anterior.")
        compose.onNodeWithText("Sistema").assertIsSelected()
        appearance.failWrite = false
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        compose.waitUntil(10_000) { appearance.themeMode.value == ThemeMode.DARK }
        compose.onNodeWithText("Oscuro").assertIsSelected()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test fun changingAppearancePreservesAnOpenDraftAndItsTypedTitle() {
        val restoration = launch()
        compose.onNodeWithText("Nuevo formulario").performClick()
        compose.onNodeWithText("Formulario de pedido").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasSetTextAction() and hasText("Formulario de pedido")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction() and hasText("Formulario de pedido")).performTextReplacement("Borrador conservado")
        runBlocking { appearance.setThemeMode(ThemeMode.DARK) }
        compose.waitUntil(10_000) { dark.get() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction() and hasText("Borrador conservado")).assertExists()
        compose.onNodeWithText("Editar formulario").assertExists()
    }

    private fun launch(): StateRestorationTester = StateRestorationTester(compose).also { restoration ->
        restoration.setContent {
            SpanishTestTheme {
                val configuration = Configuration(LocalConfiguration.current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (systemDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                CompositionLocalProvider(LocalConfiguration provides configuration) {
                    AppearanceRoot(appearance) {
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        SideEffect { dark.set(isDark) }
                        key(owner) {
                            FormLinkRoute(forms, templates, requests, accountContent = { onBack ->
                                Text("Cuenta de prueba")
                                Button(onClick = onBack) { Text("Regresar") }
                            })
                        }
                    }
                }
            }
        }
        waitForText("Mis formularios")
    }

    private fun openSettings() {
        compose.onNodeWithText("Configuración").performClick()
        waitForText("Apariencia")
    }

    private fun select(label: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(label) and isSelected()).fetchSemanticsNodes().size == 1
        }
    }

    private fun waitForText(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun back() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private class TestAppearanceRepository : AppearanceRepository {
        override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
        @Volatile var failWrite = false
        override suspend fun setThemeMode(mode: ThemeMode) {
            if (failWrite) throw IOException("test write")
            themeMode.value = mode
        }
    }
}
