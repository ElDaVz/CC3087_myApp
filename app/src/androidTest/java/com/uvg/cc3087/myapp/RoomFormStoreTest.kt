package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class RoomFormStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun latePublicationUpdatesSummariesAndCoverObserversWithinOwnerPartition() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            local.mergeRemote("a", listOf(FormDraft("restored", "Recovered", emptyList(), 1, 1, ownerId = "a")))
            val summaries = kotlinx.coroutines.channels.Channel<List<FormSummary>>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            val urls = kotlinx.coroutines.channels.Channel<String?>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            val cards = launch { local.observe("a").collect { summaries.send(it) } }
            val cover = launch { local.observePublishedCoverUrl("a", "restored").collect { urls.send(it) } }
            try {
                withTimeout(5000) {
                    assertNull(summaries.receive().single().publishedCoverUrl)
                    assertNull(urls.receive())
                    val remote = "https://res.cloudinary.com/uxrkjzti/image/upload/v1/cover.jpg"
                    db.cloud().savePublication(PublicationEntity("restored", "a", "ARCHIVED", "link", 1, 1, "hash", 1, remote))
                    var card = summaries.receive().single()
                    while (card.publishedCoverUrl == null) card = summaries.receive().single()
                    assertEquals(remote, card.publishedCoverUrl)
                    assertEquals("", card.imageUrl)
                    assertEquals(FormStatus.DRAFT, card.status)
                    var url = urls.receive()
                    while (url == null) url = urls.receive()
                    assertEquals(remote, url)
                    assertNull(local.observePublishedCoverUrl("b", "restored").first())
                    assertNull(local.observePublishedCoverUrl(null, "restored").first())
                    assertTrue(local.observe("b").first().isEmpty())
                    local.save("a", checkNotNull(local.get("a", "restored")).copy(coverAction = "REMOVE"))
                    assertEquals("REMOVE", local.observe("a").first().single().coverAction)
                }
            } finally { cards.cancelAndJoin(); cover.cancelAndJoin(); summaries.close(); urls.close() }
        } finally { db.close() }
    }
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
