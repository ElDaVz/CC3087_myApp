package com.uvg.cc3087.myapp.domain.search

import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FormSearchTest {
    private fun form(title: String) = FormSummary(title, title, 0, "", FormStatus.DRAFT, "")
    private suspend fun matches(query: String, vararg titles: String) =
        FormSearch.find(titles.map(::form), query).map { it.title }

    @Test fun relevanceRanksExactPrefixPartialThenFuzzyWithStableTies() = runTest {
        assertEquals(
            listOf("Pedido", "Pedido mensual", "Mi pedido", "Tu pedido", "Pedida"),
            matches("pedido", "Pedida", "Mi pedido", "Pedido mensual", "Tu pedido", "Pedido")
        )
    }

    @Test fun normalizationHandlesCaseAccentsWhitespaceAndDecomposedUnicode() = runTest {
        assertEquals(listOf("INSCRIPCIÓN mensual", "Inscripcio\u0301n mensual"),
            matches("  inscripción   MENSUAL  ", "INSCRIPCIÓN mensual", "Inscripcio\u0301n mensual", "Mensual"))
    }

    @Test fun everyWordIsRequiredAndWordOrderIsFree() = runTest {
        assertEquals(listOf("Mensual de pedidos"),
            matches("pedido mensual", "Pedidos", "Mensual", "Mensual de pedidos"))
        assertEquals(listOf("Inscripción"), matches("inscri", "Inscripción"))
    }

    @Test fun supportsAllFourEditsAndThresholdBoundaries() = runTest {
        for (query in listOf("pediod", "peddo", "peddido", "pedida")) {
            assertEquals(query, listOf("Pedido"), matches(query, "Pedido"))
        }
        assertEquals(emptyList<String>(), matches("pan", "Paz"))
        assertEquals(listOf("Casa"), matches("cosa", "Casa"))
        assertEquals(emptyList<String>(), matches("cosa", "Cama"))
        assertEquals(listOf("Mensual"), matches("mensualx", "Mensual"))
        assertEquals(emptyList<String>(), matches("mensual", "Mxnsuxl"))
        assertEquals(listOf("Inscripción"), matches("inscripcxxn", "Inscripción"))
        assertEquals(emptyList<String>(), matches("inscripcxxx", "Inscripción"))
    }

    @Test fun blankQueryKeepsOriginalOrderAndLongUnrelatedWordsDoNotMatch() = runTest {
        assertEquals(listOf("Z", "A"), matches(" \t ", "Z", "A"))
        assertEquals(emptyList<String>(), matches("a".repeat(5_000), "Pedido"))
        assertEquals(emptyList<String>(), matches("pedido", "X".repeat(5_000)))
    }
}
