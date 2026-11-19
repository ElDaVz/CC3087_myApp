package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.FormFieldWire
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

class FieldAnalyticsRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun localTypedSnapshotsSurviveReopenDraftEditsAndDeletionWithoutMarkingRead() = runBlocking {
        val name = "field-analytics-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, FormLinkDatabase::class.java, name).build()
        val field = FormFieldDraft("quantity", FormFieldType.NUMBER, "Cantidad", unit = "kg")
        val form = FormDraft("f", "Pedido", listOf(field), 1, 1)
        try {
            open().let { db -> try {
                db.guestFormRepository().saveDraft(form)
                val request = RoomRequestRepository(db).submit(form, "one", mapOf("quantity" to FormAnswer(text = "1,5")), Locale.GERMANY)
                assertEquals(BigDecimal("1.5"), request.answers.single().numericValue)
                db.guestFormRepository().saveDraft(form.copy(fields = listOf(field.copy(unit = "lb"))))
                db.guestFormRepository().deleteDraft("f")
            } finally { db.close() } }
            open().let { db -> try {
                val repository = RoomRequestRepository(db)
                val snapshot = repository.observeDetails("f").first().single().answers.single()
                assertEquals(FormFieldType.NUMBER, snapshot.field!!.type)
                assertEquals("kg", snapshot.field!!.unit)
                assertEquals(RequestAnswerValue.Text("1,5"), snapshot.value)
                assertEquals(BigDecimal("1.5"), snapshot.numericValue)
                assertTrue(repository.observeDetails("other").first().isEmpty())
                assertTrue(db.requestReads().get("guest").isEmpty())
            } finally { db.close() } }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun legacyJsonRemainsReadableWithoutInventingAComponentType() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            db.requests().insert(RequestEntity("f", "old", "Antiguo", 1, "PENDING",
                """[{"fieldId":"q","label":"Cantidad","kind":"text","value":"150"}]"""))
            val answer = RoomRequestRepository(db).observeDetails("f").first().single().answers.single()
            assertNull(answer.field)
            assertNull(answer.numericValue)
            assertEquals(RequestAnswerValue.Text("150"), answer.value)
        } finally { db.close() }
    }

    @Test fun remoteMetadataPreservesAllNineTypesAndSelectionConfiguration() {
        FormFieldType.entries.forEach { type ->
            val field = FormFieldDraft("q", type, "Pregunta", options = if (type.hasOptions)
                listOf(FieldOption("a", "A"), FieldOption("b", "B")) else emptyList(), unit = "kg", includeTime = true,
                allowMultipleAnswers = type == FormFieldType.CHECKBOX_GROUP || type == FormFieldType.DROPDOWN)
            val json = JSONObject().withRemoteFieldSnapshot(FormFieldWire.encode(field, 0))
            val snapshot = JSONObject(json.toString()).readFieldSnapshot()!!
            assertEquals(type, snapshot.type)
            assertEquals(if (type.hasOptions) listOf("A", "B") else emptyList<String>(), snapshot.optionLabels)
            assertEquals(type == FormFieldType.DATE, snapshot.includeTime)
            assertEquals(field.isMultipleSelection, snapshot.multipleAnswers)
            assertEquals(if (type == FormFieldType.NUMBER) "kg" else "", snapshot.unit)
        }
        assertNull(JSONObject().withRemoteFieldSnapshot(mapOf("type" to "FUTURE_TYPE")).readFieldSnapshot())
    }

    @Test fun webNumericStringsAndRemotePrimitiveNumbersPreserveTheirMagnitude() {
        assertEquals(BigDecimal("1.5"), remoteAnswerNumber("1.5"))
        assertEquals(BigDecimal("-1.50"), remoteAnswerNumber("-1.50"))
        assertEquals(BigDecimal("42"), remoteAnswerNumber(42L))
        assertEquals(BigDecimal("1.5"), remoteAnswerNumber(1.5))
        assertNull(remoteAnswerNumber("1,5"))
        assertNull(remoteAnswerNumber("1,000"))
        assertNull(remoteAnswerNumber(Double.NaN))
        assertNull(remoteAnswerNumber(""))
        assertNull(remoteAnswerNumber(true))
    }

    @Test fun accountObservationsAreOwnerAndFormScopedAndNeverImportGuestResponses() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        var active = "a"
        val http = FirebaseHttp(true)
        val client = FirestoreRest("demo-formlink", RestAuthRepository(null, DeviceCipher(context), http, true), http, true)
        try {
            val field = FormFieldDraft("q", FormFieldType.NUMBER, "Cantidad", unit = "kg")
            val answer = JSONObject().put("fieldId", "q").put("label", "Cantidad").put("kind", "text").put("value", "1.5")
                .withRemoteFieldSnapshot(FormFieldWire.encode(field, 0)).put("number", remoteAnswerNumber("1.5")!!.toPlainString())
            for (owner in listOf("a", "b")) for (form in listOf("f", "other"))
                db.cloud().saveRequest(RemoteRequestEntity(owner, form, "same", "$owner/$form", 1, "PENDING", "[$answer]"))
            db.requests().insert(RequestEntity("f", "guest", "Guest", 1, "PENDING", "[]"))
            val a = CloudRequestRepository(db, client, "a", { active }, {})
            val snapshot = a.observeDetails("f").first().single()
            assertEquals("a/f", snapshot.formTitle)
            assertEquals(BigDecimal("1.5"), snapshot.answers.single().numericValue)
            assertEquals("kg", snapshot.answers.single().field!!.unit)
            assertTrue(db.requestReads().get("account:a").isEmpty())
            active = "b"
            assertTrue(runCatching { a.observeDetails("f").first() }.exceptionOrNull() is CancellationException)
            val b = CloudRequestRepository(db, client, "b", { active }, {})
            assertEquals("b/f", b.observeDetails("f").first().single().formTitle)
        } finally { db.close() }
    }
}
