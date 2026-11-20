package com.uvg.cc3087.myapp

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.state.FormsEmptyReason
import com.uvg.cc3087.myapp.ui.state.FormsLoadStatus
import com.uvg.cc3087.myapp.ui.state.FormsUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class FormsSearchPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightDarkAndLargeTextKeepSearchAndRecoveryReachable() {
        var dark by mutableStateOf(false)
        var fontScale by mutableFloatStateOf(1f)
        var empty by mutableStateOf(false)
        compose.setContent {
            SpanishTestTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    MyappTheme(darkTheme = dark) {
                        Forms(FormsUiState(
                            forms = if (empty) emptyList() else listOf(
                                FormSummary("synthetic", "Pedido mensual", 3, "19 nov", FormStatus.DRAFT, "")
                            ),
                            searchQuery = "pediod", searchExpanded = true,
                            status = if (empty) FormsLoadStatus.EMPTY else FormsLoadStatus.CONTENT,
                            emptyReason = FormsEmptyReason.SEARCH
                        ))
                    }
                }
            }
        }
        compose.onNodeWithTag("forms-search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Limpiar búsqueda").assertIsDisplayed()
        capture("forms-search-light")
        compose.runOnIdle { dark = true }
        compose.onNodeWithContentDescription("Cerrar búsqueda").assertIsDisplayed()
        capture("forms-search-dark")
        compose.runOnIdle { fontScale = 1.5f }
        compose.onNodeWithText("Nuevo formulario").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Borradores").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Cerrar búsqueda").assertIsDisplayed()
        capture("forms-search-large-text")
        compose.runOnIdle { empty = true }
        compose.onNodeWithText("No hay formularios que coincidan con tu búsqueda").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Mostrar todos los formularios").performScrollTo().assertIsDisplayed()
        capture("forms-search-empty")
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("search-evidence")!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
