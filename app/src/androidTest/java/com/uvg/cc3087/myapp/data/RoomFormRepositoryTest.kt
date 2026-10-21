package com.uvg.cc3087.myapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.local.toEntity
import com.uvg.cc3087.myapp.data.local.toFieldEntities
import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.repository.RoomFormRepository
import java.util.UUID
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RoomFormRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: FormLinkDatabase
    private lateinit var repository: RoomFormRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        repository = RoomFormRepository(database.drafts())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun everyFieldTypeAndConfigurationSurvivesStorage() = runBlocking {
        val draft = draft("all-types")
        repository.saveDraft(draft)

        assertEquals(draft, repository.getDraft(draft.id))
        assertNull(repository.getDraft("missing"))
    }

    @Test
    fun updatingReplacesDeletedFieldsAndKeepsTheNewOrder() = runBlocking {
        val original = draft("editable")
        repository.saveDraft(original)
        val updated = original.copy(
            title = "",
            fields = original.fields.takeLast(2).reversed(),
            updatedAt = 300
        )
        repository.saveDraft(updated)

        // un borrador incompleto también debe poder recuperarse
        assertEquals(updated, repository.getDraft(original.id))
        repository.saveDraft(updated.copy(fields = emptyList()))
        assertTrue(repository.getDraft(original.id)!!.fields.isEmpty())
    }

    @Test
    fun differentDraftsCanReuseTemplateFieldIdsWithoutMixingTheirValues() = runBlocking {
        val first = draft("one")
        val second = draft("two").copy(fields = first.fields.map { it.copy(title = "Otro: ${it.title}") })
        repository.saveDraft(first)
        repository.saveDraft(second)
        repository.deleteDraft(first.id)

        assertNull(repository.getDraft(first.id))
        assertEquals(second, repository.getDraft(second.id))
        database.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM form_draft_fields WHERE formId = ?", arrayOf(first.id)
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun observersReceiveInsertUpdateAndDeleteWithoutResubscribing() = runBlocking {
        withTimeout(10_000) {
            val emissions = repository.observeForms().produceIn(this)
            try {
                assertTrue(emissions.receive().isEmpty())
                val original = draft("observed")
                repository.saveDraft(original)
                val summary = emissions.receive().single()
                assertEquals(original.id, summary.id)
                assertEquals(FormStatus.DRAFT, summary.status)
                assertEquals(0, summary.responseCount)

                repository.saveDraft(original.copy(title = "Nuevo título", updatedAt = 400))
                assertEquals("Nuevo título", emissions.receive().single().title)
                repository.deleteDraft(original.id)
                assertTrue(emissions.receive().isEmpty())
                repository.deleteDraft("missing")
            } finally {
                emissions.cancel()
            }
        }
    }

    @Test
    fun summariesAreOrderedByLastUpdateWithStableIdTieBreaker() = runBlocking {
        repository.saveDraft(draft("z").copy(updatedAt = 200))
        repository.saveDraft(draft("b").copy(updatedAt = 300))
        repository.saveDraft(draft("a").copy(updatedAt = 300))
        withTimeout(10_000) {
            val emissions = repository.observeForms().produceIn(this)
            try {
                assertEquals(listOf("a", "b", "z"), emissions.receive().map { it.id })
            } finally {
                emissions.cancel()
            }
        }
    }

    @Test
    fun aFailedFieldInsertRollsBackTheWholeDraftUpdate() = runBlocking {
        val original = draft("atomic")
        repository.saveDraft(original)
        val fields = original.toFieldEntities()
        try {
            database.drafts().saveDraft(
                original.toEntity().copy(title = "No debe quedar guardado"),
                listOf(fields[0], fields[0].copy(position = 1))
            )
            fail("The duplicate field id should fail")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            assertEquals(original, repository.getDraft(original.id))
        }
    }

    @Test
    fun aDuplicateFieldIdIsRejectedWithoutOverwritingTheSavedDraft() = runBlocking {
        val original = draft("unique-fields")
        repository.saveDraft(original)
        try {
            repository.saveDraft(original.copy(fields = listOf(original.fields[0], original.fields[0])))
            fail("Duplicate ids should not be saved")
        } catch (_: IllegalArgumentException) {
            assertEquals(original, repository.getDraft(original.id))
        }
    }

    @Test
    fun reopeningTheDatabaseKeepsTheDraftOnDisk() = runBlocking {
        // esta base es exclusiva de la prueba, no abrimos ni borramos los datos de la app
        val testDatabaseName = "test-drafts-${UUID.randomUUID()}.db"
        val original = draft("persistent")
        var diskDatabase: FormLinkDatabase? = null
        try {
            diskDatabase = Room.databaseBuilder(context, FormLinkDatabase::class.java, testDatabaseName).build()
            RoomFormRepository(diskDatabase.drafts()).saveDraft(original)
            diskDatabase.close()
            diskDatabase = Room.databaseBuilder(context, FormLinkDatabase::class.java, testDatabaseName).build()

            assertEquals(original, RoomFormRepository(diskDatabase.drafts()).getDraft(original.id))
        } finally {
            diskDatabase?.close()
            context.deleteDatabase(testDatabaseName)
        }
    }

    private fun draft(id: String) = FormDraft(
        id = id,
        title = "Pedido \"especial\"\nCafé",
        createdAt = 100,
        updatedAt = 200,
        fields = FormFieldType.entries.mapIndexed { index, type ->
            FormFieldDraft(
                id = "field-$index", type = type, title = "Pregunta $index",
                required = index % 2 == 0, allowMultipleAnswers = true,
                options = listOf(FieldOption("a\"$index", "Café\ncon leche"), FieldOption("b$index", "")),
                helpText = "Ayuda: $index", placeholder = "Escribe aquí",
                includeTime = false, disallowPastDates = true, allowDecimals = false,
                minimum = "-3", maximum = "20", unit = "unidades"
            )
        }
    )
}
