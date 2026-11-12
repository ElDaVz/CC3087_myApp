package com.uvg.cc3087.myapp

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.PublicationError
import com.uvg.cc3087.myapp.ui.resources.messageRes
import com.uvg.cc3087.myapp.ui.screens.SaveShareScreen
import com.uvg.cc3087.myapp.ui.state.SaveShareUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class LocaleResourcesTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun configuration(tag: String) = Configuration(context.resources.configuration).apply {
        setLocales(LocaleList(Locale.forLanguageTag(tag)))
    }

    @Test fun androidSelectsSpanishForRegionalLocalesAndEnglishAsFallback() {
        for ((tag, publish, retry) in listOf(
            Triple("es-GT", "Publicar", "Reintentar"),
            Triple("es-ES", "Publicar", "Reintentar"),
            Triple("en-US", "Publish", "Retry"),
            Triple("fr-FR", "Publish", "Retry")
        )) {
            val resources = context.createConfigurationContext(configuration(tag)).resources
            assertEquals(tag, publish, resources.getString(R.string.publish))
            assertEquals(tag, retry, resources.getString(R.string.retry))
            for (error in PublicationError.entries) {
                assertTrue(resources.getString(error.messageRes).isNotBlank())
            }
        }
    }

    @Test fun publicationLabelsAndExistingErrorsFollowConfigurationChanges() {
        val locale = mutableStateOf("en-US")
        val state = SaveShareUiState(
            draft = FormDraft("locale-test", "Customer title", emptyList(), 100, 200),
            publishError = PublicationError.SIGN_IN_REQUIRED
        )
        compose.setContent {
            val config = configuration(locale.value)
            val resources = context.createConfigurationContext(config).resources
            CompositionLocalProvider(LocalConfiguration provides config, LocalResources provides resources) {
                MyappTheme {
                    SaveShareScreen(state, {}, {}, {}, { true }, { true }, onPublish = {})
                }
            }
        }
        compose.onNodeWithText("Publish").assertExists()
        compose.onNodeWithText("Sign in to publish").assertExists()
        compose.runOnIdle { locale.value = "es-GT" }
        compose.onNodeWithText("Publicar").assertExists()
        compose.onNodeWithText("Inicia sesión para publicar").assertExists()
        compose.runOnIdle { locale.value = "en-US" }
        compose.onNodeWithText("Publish").assertExists()
        compose.onNodeWithText("Sign in to publish").assertExists()
    }
}
