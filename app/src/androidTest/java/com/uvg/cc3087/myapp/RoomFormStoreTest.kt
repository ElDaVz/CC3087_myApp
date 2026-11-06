package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RoomFormStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun importingIsExplicitAndOwnerChecksProtectDirectIds() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            val guest = FormDraft("guest", "Pedido", emptyList(), 1, 1, coverPath = "covers/local.jpg")
            local.save(null, guest)
            assertNull(local.get("a", "guest"))
            assertTrue(local.pending("a").isEmpty())
            local.importGuests("a", setOf("guest"))
            assertNull(local.get(null, "guest"))
            assertNull(local.get("b", "guest"))
            assertEquals("covers/local.jpg", local.get("a", "guest")!!.coverPath)
            assertEquals("a", local.pending("a").single().draft.ownerId)
            assertTrue(runCatching { local.save("b", guest) }.isFailure)
            assertTrue(runCatching { local.delete("b", "guest") }.isFailure)
            assertTrue(runCatching { local.importGuests("b", setOf("guest")) }.isFailure)
        } finally { db.close() }
    }
    @Test fun acknowledgementsCannotClearNewerEditsAndRemoteDownloadsPreserveCovers() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            val form = FormDraft("f", "One", emptyList(), 1, 1, "covers/local.jpg", "a")
            local.save("a", form)
            val first = local.pending("a").single()
            local.save("a", form.copy(title = "Two"))
            local.acknowledge("a", first)
            assertEquals(2L, local.pending("a").single().revision)
            local.mergeRemote("a", listOf(form.copy(title = "Old server copy", coverPath = null)))
            assertEquals("Two", local.get("a", "f")!!.title)
            local.acknowledge("a", local.pending("a").single())
            local.mergeRemote("a", listOf(form.copy(title = "Server", coverPath = null)))
            assertEquals("Server", local.get("a", "f")!!.title)
            assertEquals("covers/local.jpg", local.get("a", "f")!!.coverPath)
            local.delete("a", "f")
            assertNull(local.get("a", "f"))
            val deletion = local.pending("a").single()
            assertTrue(deletion.deleted)
            local.acknowledge("a", deletion)
            assertTrue(local.pending("a").isEmpty())
            assertNull(db.drafts().getDraft("f"))
        } finally { db.close() }
    }
}
