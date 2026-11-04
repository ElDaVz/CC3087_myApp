package com.uvg.cc3087.myapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.cc3087.myapp.data.model.RequestAnswerValue
import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.model.RequestDetailAnswer
import com.uvg.cc3087.myapp.ui.screens.RequestDetailScreen
import com.uvg.cc3087.myapp.ui.state.RequestDetailLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RequestDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rendersFalseAndMissingAnswersWithoutConfusingThem() {
        val detail = RequestDetail("form", "response", "Pedido", null, listOf(
            RequestDetailAnswer("delivery", "Entrega", RequestAnswerValue.Checked(false)),
            RequestDetailAnswer("notes", "Notas", RequestAnswerValue.Text(""))
        ))
        var back = 0
        compose.setContent {
            SpanishTestTheme {
                RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.CONTENT, detail), { back++ }, {})
            }
        }
        compose.onNodeWithText("Estado: Sin especificar").assertExists()
        compose.onNodeWithText("No").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sin respuesta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, back)
    }

    @Test fun loadFailureOffersRetryAndNoCustomerData() {
        var retries = 0
        compose.setContent {
            SpanishTestTheme {
                RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.ERROR), {}, { retries++ })
            }
        }
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
        compose.onNodeWithText("Respuestas del cliente").assertDoesNotExist()
    }

    @Test fun emptyRequestIsDifferentFromRequestNotFound() {
        val detail = RequestDetail("form", "response", "Pedido", null, emptyList())
        compose.setContent {
            SpanishTestTheme {
                RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.CONTENT, detail), {}, {})
            }
        }
        compose.onNodeWithText("Esta solicitud no contiene respuestas.").assertExists()
        compose.onNodeWithText("Esta solicitud ya no está disponible.").assertDoesNotExist()
    }
}
