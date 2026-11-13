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

    @Test fun confirmedDeletionSurvivesAccountSwitchAndRejectsRestoredEditorSaves() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            val auth = object : com.uvg.cc3087.myapp.data.repository.AuthRepository {
                override val configured = true
                override val state = MutableStateFlow<AuthState>(AuthState.SignedIn(UserSession("a", "a@example.com")))
                override suspend fun signIn(email: String, password: String) = Unit
                override suspend fun signUp(email: String, password: String) = Unit
                override suspend fun sendPasswordReset(email: String) = Unit
                override suspend fun signOut() { state.value = AuthState.Guest }
            }
            fun repository(owner: String?) = com.uvg.cc3087.myapp.data.repository.OfflineFirstFormRepository(local, auth, owner) {}
            // A newly created editor can still hold ownerId=null before reloading Room.
            val editorSnapshot = FormDraft("deleted", "Old account form", emptyList(), 1, 1)
            val accountA = repository("a")
            accountA.saveDraft(editorSnapshot)
            val unrelated = editorSnapshot.copy(id = "keep", title = "Keep this form")
            accountA.saveDraft(unrelated)
            local.acknowledge("a", local.pending("a").first { it.draft.id == "keep" })
            accountA.deleteDraft(editorSnapshot.id)
            local.acknowledge("a", local.pending("a").single())
            // Previously the acknowledgement removed the only record of the deletion.
            assertTrue(runCatching { accountA.saveDraft(editorSnapshot) }.isFailure)
            auth.signOut()
            assertTrue(repository(null).observeForms().first().isEmpty())
            auth.state.value = AuthState.SignedIn(UserSession("b", "b@example.com"))
            val accountB = repository("b")
            assertTrue(accountB.observeForms().first().isEmpty())
            assertTrue(runCatching { accountA.saveDraft(editorSnapshot) }.isFailure)
            assertTrue(runCatching { accountB.saveDraft(editorSnapshot) }.isFailure)
            accountB.saveDraft(editorSnapshot.copy(id = "new-b", title = "New account form"))
            assertEquals(listOf("new-b"), accountB.observeForms().first().map { it.id })
            auth.signOut()
            auth.state.value = AuthState.SignedIn(UserSession("a", "a@example.com"))
            assertEquals(listOf("keep"), repository("a").observeForms().first().map { it.id })
            assertTrue(local.pending("a").isEmpty())
        } finally { db.close() }
    }

    @Test fun deletedGuestCannotBeRestoredOrImportedIntoAnotherAccount() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            val snapshot = FormDraft("guest", "Guest form", emptyList(), 1, 1)
            local.save(null, snapshot)
            local.delete(null, snapshot.id)
            assertTrue(runCatching { local.save(null, snapshot) }.isFailure)
            assertTrue(runCatching { local.save("b", snapshot) }.isFailure)
            assertTrue(runCatching { local.importGuests("b", setOf(snapshot.id)) }.isFailure)
            assertNull(local.get(null, snapshot.id))
            assertTrue(local.observe(null).first().isEmpty())
            assertTrue(local.observe("b").first().isEmpty())
        } finally { db.close() }
    }

    @Test fun staleDownloadsCannotReviveAcknowledgedDeletionsOrDiscardTheirMarkers() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(db)
            val form = FormDraft("f", "Deleted form", emptyList(), 1, 1, ownerId = "a")
            local.save("a", form)
            local.delete("a", form.id)
            local.acknowledge("a", local.pending("a").single())
            local.mergeRemote("a", listOf(form))
            assertNull(local.get("a", form.id))
            local.mergeRemote("a", emptyList())
            assertTrue(runCatching { local.save("a", form) }.isFailure)
            assertTrue(local.pending("a").isEmpty())
            // Downloaded removals must protect against a restored editor as well.
            val remote = form.copy(id = "removed-remotely")
            local.mergeRemote("a", listOf(remote))
            local.mergeRemote("a", emptyList())
            assertTrue(runCatching { local.save("a", remote) }.isFailure)
            assertTrue(local.observe("a").first().isEmpty())
        } finally { db.close() }
    }

    @Test fun deletionCascadesFieldsAndUnreferencedCoversButPreservesRequestsAndHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        val removed = mutableListOf<String>()
        val covers = object : com.uvg.cc3087.myapp.data.repository.CoverStorage {
            override suspend fun importImage(source: String) = error("unused")
            override suspend fun delete(path: String) { removed += path }
        }
        try {
            val local = RoomFormStore(db, covers)
            val form = FormDraft("f", "Snapshot", listOf(FormFieldDraft("q", FormFieldType.TEXT, "Question")),
                1, 1, coverPath = "covers/shared.jpg", ownerId = "a")
            local.save("a", form)
            local.save(null, form.copy(id = "guest", ownerId = null))
            db.cloud().saveRequest(RemoteRequestEntity("a", "f", "r", "Snapshot", 1, "COMPLETED", "[]"))
            val read = RequestReadEntity("account:a", "f", "r")
            db.requestReads().markRead(read)
            db.cloud().saveCoverUpload(CoverUploadEntity("a", "f", "hash", "url", "public-id"))
            local.delete("a", "f")
            assertNull(local.get("a", "f"))
            assertTrue(local.observe("a").first().isEmpty())
            assertTrue(db.drafts().getDraft("f")!!.fields.isEmpty())
            assertTrue(removed.isEmpty()) // The guest still references the same file.
            val pending = local.pending("a").single()
            local.delete("a", "f")
            assertEquals(pending.revision, local.pending("a").single().revision)
            assertTrue(runCatching { local.save("a", form) }.isFailure)
            local.delete(null, "guest")
            assertEquals(listOf("covers/shared.jpg"), removed)
            local.acknowledge("a", pending)
            assertTrue(db.drafts().getDraft("f")!!.form.deleted)
            assertTrue(local.pending("a").isEmpty())
            assertEquals("Snapshot", db.cloud().detail("a", "f", "r")!!.formTitle)
            assertEquals(listOf(read), db.requestReads().get("account:a"))
            assertNotNull(db.cloud().uploadedCover("a", "f", "hash"))
        } finally { db.close() }
    }

    @Test fun accountDeletionReleasesPrivateCoverBeforeSynchronization() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        val removed = mutableListOf<String>()
        val covers = object : com.uvg.cc3087.myapp.data.repository.CoverStorage {
            override suspend fun importImage(source: String) = error("unused")
            override suspend fun delete(path: String) { removed += path }
        }
        try {
            val local = RoomFormStore(db, covers)
            local.save("a", FormDraft("f", "Title", emptyList(), 1, 1, "covers/private.jpg", "a"))
            local.delete("a", "f")
            assertEquals(listOf("covers/private.jpg"), removed)
            assertTrue(local.pending("a").single().deleted)
        } finally { db.close() }
    }

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
            assertTrue(db.drafts().getDraft("f")!!.form.deleted)
        } finally { db.close() }
    }
}
