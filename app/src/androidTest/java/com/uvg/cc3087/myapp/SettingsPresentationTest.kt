package com.uvg.cc3087.myapp

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.model.ThemeMode
import com.uvg.cc3087.myapp.ui.screens.SettingsScreen
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.AppearanceUiState
import java.io.File
import org.junit.Rule
import org.junit.Test

class SettingsPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightDarkAndLargeTextKeepAllControlsReachable() {
        var dark by mutableStateOf(false)
        var fontScale by mutableFloatStateOf(1f)
        var mode by mutableStateOf(ThemeMode.SYSTEM)
        compose.setContent {
            SpanishTestTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    MyappTheme(darkTheme = dark) {
                        SettingsScreen(AppearanceUiState(mode = mode, loaded = true),
                            onTheme = { mode = it }, onRetry = {}, onBack = {}, onAccount = {}, onStatistics = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Claro").performScrollTo().performClick().assertIsSelected()
        capture("settings-light")
        compose.runOnIdle { dark = true }
        compose.onNodeWithText("Oscuro").performScrollTo().performClick().assertIsSelected()
        capture("settings-dark")
        compose.runOnIdle { fontScale = 1.5f }
        compose.onNodeWithText("Cuenta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Estadísticas").performScrollTo().assertIsDisplayed()
        capture("settings-large-text-top")
        compose.onNodeWithText("Oscuro").performScrollTo().assertIsDisplayed().assertIsSelected()
        capture("settings-large-text-bottom")
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("settings-evidence")!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
