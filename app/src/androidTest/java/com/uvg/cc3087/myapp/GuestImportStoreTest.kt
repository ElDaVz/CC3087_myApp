package com.uvg.cc3087.myapp

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.GuestImportCounts
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import com.uvg.cc3087.myapp.data.security.EncryptedDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class GuestImportStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun draft(id: String) = FormDraft(id, "Draft", listOf(FormFieldDraft("q-$id", FormFieldType.TEXT, "Question")), 1, 2)

    @Test fun reservedDraftsAndEncryptedImagesSurviveReopenAndTransferOnce() = runBlocking {
        val name = "guest-import-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, FormLinkDatabase::class.java, name)
            .openHelperFactory(EncryptedDatabase.factory(context, name)).build()
        suspend fun withOpen(block: suspend (FormLinkDatabase) -> Unit) {
            val db = open()
            try { block(db) } finally { db.close() }
        }
        val source = File(context.cacheDir, "guest-cover-${UUID.randomUUID()}.jpg")
        val image = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        source.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, 85, it) }; image.recycle()
        val covers = PrivateCoverStorage(context)
        val path = covers.importImage(android.net.Uri.fromFile(source).toString())
        try {
            val before = File(context.filesDir, path).readBytes()
            val blur = File(context.filesDir, "$path.blur.jpg").readBytes()
            withOpen { db ->
                val local = RoomFormStore(db, covers)
                local.save(null, draft("one").copy(coverPath = path, coverAction = "REPLACE"))
                local.save(null, draft("two"))
                local.save("other", draft("private").copy(ownerId = "other"))
                val imports = RoomGuestImportStore(db)
                assertEquals(setOf("one", "two"), imports.snapshotGuestIds())
                imports.prepare("a", imports.snapshotGuestIds())
                assertTrue(imports.snapshotGuestIds().isEmpty())
                assertEquals(GuestImportCounts(2, 0), imports.observe("a").first())
                assertEquals(GuestImportCounts(), imports.observe("b").first())
                assertTrue(runCatching { local.importGuests("b", setOf("one")) }.isFailure)
            }
            withOpen { db ->
                val local = RoomFormStore(db, covers)
                val restored = RoomGuestImportStore(db)
                assertEquals(2, restored.transfer("a"))
                assertEquals(0, restored.transfer("a"))
                assertEquals(GuestImportCounts(0, 2), restored.observe("a").first())
                val form = checkNotNull(local.get("a", "one"))
                assertEquals(path, form.coverPath); assertEquals("REPLACE", form.coverAction)
                assertEquals(draft("one").fields, form.fields)
                assertEquals(1L, form.createdAt); assertEquals(2L, form.updatedAt)
                assertNull(local.get(null, "one")); assertNull(local.get("b", "one"))
                assertNotNull(local.get("other", "private"))
                assertEquals(2L, local.pending("a").first { it.draft.id == "one" }.revision)
                assertNull(db.cloud().publication("a", "one"))
                assertTrue(db.requests().observeCounts().first().isEmpty())
            }
            assertArrayEquals(before, File(context.filesDir, path).readBytes())
            assertArrayEquals(blur, File(context.filesDir, "$path.blur.jpg").readBytes())
            assertTrue(DeviceCipher(context).decrypt(before).isNotEmpty())
        } finally { source.delete(); covers.delete(path); context.deleteDatabase(name) }
    }

    @Test fun aConflictingOwnerRollsBackEveryTransferAndKeepsReservations() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            local.save(null, draft("first")); local.save("b", draft("z-conflict").copy(ownerId = "b"))
            db.guestImports().reserve(GuestImportEntity("first", "a"))
            db.guestImports().reserve(GuestImportEntity("z-conflict", "a"))
            val imports = RoomGuestImportStore(db)
            assertTrue(runCatching { imports.transfer("a") }.isFailure)
            assertNotNull(local.get(null, "first")); assertNull(local.get("a", "first"))
            assertNotNull(local.get("b", "z-conflict"))
            assertEquals(GuestImportCounts(2, 0), imports.observe("a").first())
        } finally { db.close() }
    }

    @Test fun newGuestDraftsAreNotCapturedByRestoringAnOldReservation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            val imports = RoomGuestImportStore(db)
            local.save(null, draft("old"))
            imports.prepare("a", imports.snapshotGuestIds())
            local.save(null, draft("new"))
            imports.prepare("b", setOf("old")) // Cannot replace another account's reservation.
            assertEquals(0, imports.transfer("b"))
            assertEquals(1, imports.transfer("a"))
            assertNotNull(local.get(null, "new"))
            assertEquals(setOf("new"), imports.snapshotGuestIds())
            imports.prepare("b", imports.snapshotGuestIds())
            assertEquals(1, imports.transfer("b"))
            assertNull(local.get("b", "old")); assertNull(local.get("a", "new"))
        } finally { db.close() }
    }

    @Test fun deletedGuestsAreSkippedAndCompletedEntriesAreReconciled() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            local.save(null, draft("gone")); local.save(null, draft("kept"))
            val imports = RoomGuestImportStore(db)
            imports.prepare("a", imports.snapshotGuestIds())
            local.delete(null, "gone")
            db.drafts().claimGuest("a", "kept") // Simulate a previously applied ownership update.
            val revision = local.pending("a").single().revision
            assertEquals(0, imports.transfer("a"))
            assertEquals(revision, local.pending("a").single().revision)
            assertEquals(GuestImportCounts(0, 1), imports.observe("a").first())
        } finally { db.close() }
    }
}
