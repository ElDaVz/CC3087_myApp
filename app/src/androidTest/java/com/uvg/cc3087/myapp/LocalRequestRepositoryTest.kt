package com.uvg.cc3087.myapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import java.util.UUID

class LocalRequestRepositoryTest {
    private inline fun <T> FormLinkDatabase.useDatabase(block: (FormLinkDatabase) -> T): T =
        try { block(this) } finally { close() }
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val form = FormDraft("form", "Pedido", listOf(
        FormFieldDraft("name", FormFieldType.TEXT, "Nombre", required = true),
        FormFieldDraft("delivery", FormFieldType.CHECKBOX, "Entrega"),
        FormFieldDraft("choice", FormFieldType.MULTIPLE_CHOICE, "Sabor", options = listOf(
            FieldOption("choc", "Chocolate"), FieldOption("van", "Vainilla"))),
        FormFieldDraft("date", FormFieldType.DATE, "Fecha", includeTime = true)
    ), 1, 1)
    private val answers = mapOf("name" to FormAnswer(text = "Ana"),
        "choice" to FormAnswer(selectedIds = setOf("choc")),
        "date" to FormAnswer(date = "2030-01-10", time = "14:30"))

    @Test fun persistsAcrossReopenWithSnapshotAndStatusAndDoesNotDuplicateRetries() = runBlocking {
        val name = "requests-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, FormLinkDatabase::class.java, name).build()
        try {
            open().useDatabase { db ->
                val forms = RoomFormRepository(db.drafts(), db.requests())
                val requests = RoomRequestRepository(db)
                forms.saveDraft(form)
                val saved = requests.submit(form, "one", answers, Locale.US)
                assertEquals(RequestStatus.PENDING, saved.requestStatus)
                assertEquals(RequestAnswerValue.Checked(false), saved.answers[1].value)
                assertEquals(RequestAnswerValue.Choices(listOf("Chocolate")), saved.answers[2].value)
                assertEquals(RequestAnswerValue.Text("2030-01-10 14:30"), saved.answers[3].value)
                assertEquals(saved, requests.submit(form, "one", answers, Locale.US))
                assertEquals(1, forms.observeForms().first().single().responseCount)
                requests.updateStatus("form", "one", RequestStatus.IN_PROGRESS)
                requests.updateStatus("form", "one", RequestStatus.COMPLETED)
                forms.saveDraft(form.copy(title = "Nuevo título", fields = emptyList(), updatedAt = 2))
            }
            open().useDatabase { db ->
                val requests = RoomRequestRepository(db)
                val detail = requests.getDetail("form", "one")!!
                assertEquals("Pedido", detail.formTitle)
                assertEquals("Nombre", detail.answers.first().label)
                assertEquals(RequestAnswerValue.Text("Ana"), detail.answers.first().value)
                assertEquals(RequestStatus.COMPLETED, detail.requestStatus)
                assertEquals(1, requests.getRequests().size)
                assertNull(requests.getDetail("other", "one"))
                RoomFormRepository(db.drafts()).deleteDraft("form")
                assertEquals(detail, requests.getDetail("form", "one"))
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun rejectsInvalidAnswersAndChangedFormsWithoutCreatingRequests() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build().useDatabase { db ->
            val forms = RoomFormRepository(db.drafts())
            val requests = RoomRequestRepository(db)
            forms.saveDraft(form)
            assertTrue(runCatching { requests.submit(form, "bad", emptyMap(), Locale.US) }.isFailure)
            assertTrue(runCatching { requests.submit(form, "bad", answers + ("choice" to FormAnswer(selectedIds = setOf("unknown"))), Locale.US) }.isFailure)
            forms.saveDraft(form.copy(title = "Changed"))
            assertTrue(runCatching { requests.submit(form, "stale", answers, Locale.US) }.isFailure)
            assertTrue(requests.getRequests().isEmpty())
            assertTrue(runCatching { requests.updateStatus("form", "missing", RequestStatus.COMPLETED) }.isFailure)
        }
    }

    @Test fun migrationFromVersionOneKeepsExistingDraftsAndAddsRequestStorage() = runBlocking {
        val name = "migration-test-${UUID.randomUUID()}.db"
        val assetContext = InstrumentationRegistry.getInstrumentation().context
        val schema = assetContext.assets.open("com.uvg.cc3087.myapp.data.local.FormLinkDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        try {
            context.getDatabasePath(name).parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                    val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                    for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql")
                        .replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
                old.execSQL("INSERT INTO form_drafts VALUES ('old', 'Conservar', 1, 1)")
                old.execSQL("""INSERT INTO form_draft_fields VALUES
                    ('old', 'old-field', 0, 'TEXT', 'Pregunta anterior', 0, 0, '[]', '', '', 1, 0, 1, '', '', '')""")
                old.version = 1
            }
            Room.databaseBuilder(context, FormLinkDatabase::class.java, name)
                .addMigrations(FormLinkDatabase.MIGRATION_1_2, FormLinkDatabase.MIGRATION_2_3, FormLinkDatabase.MIGRATION_3_4, FormLinkDatabase.MIGRATION_4_5, FormLinkDatabase.MIGRATION_5_6, FormLinkDatabase.MIGRATION_6_7, FormLinkDatabase.MIGRATION_7_8).build().useDatabase { db ->
                    val forms = RoomFormRepository(db.drafts())
                    assertEquals("Conservar", forms.getDraft("old")!!.title)
                    assertEquals("Pregunta anterior", forms.getDraft("old")!!.fields.single().title)
                    forms.saveDraft(form)
                    val requests = RoomRequestRepository(db)
                    requests.submit(form, "new", answers, Locale.US)
                    assertEquals(1, requests.getRequests().size)
                }
        } finally { context.deleteDatabase(name) }
    }
}
