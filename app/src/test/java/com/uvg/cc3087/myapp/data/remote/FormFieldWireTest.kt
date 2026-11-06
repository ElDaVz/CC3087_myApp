package com.uvg.cc3087.myapp.data.remote

import com.uvg.cc3087.myapp.data.model.*
import org.junit.Assert.*
import org.junit.Test

class FormFieldWireTest {
    @Test fun everyEditorTypeAndConfigurationRoundTripsThroughThePublicContract() {
        FormFieldType.entries.forEach { type ->
            val field = FormFieldDraft("id", type, "Pregunta", required = true,
                options = listOf(FieldOption("a", "Uno"), FieldOption("b", "Dos")),
                helpText = "Ayuda", placeholder = "Ejemplo", includeTime = false,
                disallowPastDates = true, allowDecimals = false, minimum = "1", maximum = "9", unit = "kg")
            val wire = FormFieldWire.encode(field, 2)
            assertEquals(2, wire["position"])
            assertEquals(field, FormFieldWire.decode(wire))
        }
    }
    @Test fun invalidRemoteConfigurationDoesNotSilentlyEraseTheLocalDraft() {
        assertTrue(runCatching { FormFieldWire.decode(mapOf("id" to "bad", "type" to "unknown")) }.isFailure)
    }
}
