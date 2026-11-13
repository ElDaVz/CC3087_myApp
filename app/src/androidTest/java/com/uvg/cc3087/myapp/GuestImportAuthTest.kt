package com.uvg.cc3087.myapp

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.RestFormSource
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class GuestImportAuthTest {
    private val context by lazy {
        object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            private val directory = File(cacheDir, "guest-auth-${UUID.randomUUID()}").apply { mkdirs() }
            override fun getNoBackupFilesDir() = directory
        }
    }
    private val db by lazy { Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build() }
    private val store by lazy { RoomGuestImportStore(db) }
    private val local by lazy { RoomFormStore(db) }
    private fun auth(imports: GuestImportStore = store) = RestAuthRepository("demo-api-key", DeviceCipher(context),
        FirebaseHttp(true), true, guestImports = imports)
    private fun draft(id: String) = FormDraft(id, "Local draft", emptyList(), 1, 1)

    @Before fun requireEmulators() {
        Assume.assumeTrue(BuildConfig.FIREBASE_EMULATORS &&
            InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
    }
    @After fun close() { db.close(); context.noBackupFilesDir.deleteRecursively() }

    @Test fun failedLoginDoesNotReserveAndSignupTransfersOnlyAfterSessionConfirmation() = runBlocking {
        val id = "import-${UUID.randomUUID()}"
        val email = "$id@example.com"
        local.save(null, draft(id))
        val auth = auth()
        auth.state.first { it !is AuthState.Initializing }
        assertTrue(runCatching { auth.signIn(email, "password123") }.isFailure)
        assertEquals(AuthState.Guest, auth.state.value)
        assertEquals(setOf(id), store.snapshotGuestIds())
        auth.signUp(email, "password123")
        val owner = checkNotNull(auth.state.value.userId)
        assertNotNull(local.get(null, id)) // Authentication reserves; it does not transfer inline.
        assertEquals(GuestImportCounts(1, 0), store.observe(owner).first())
        val importer = GuestDraftImporter(store, { auth.state.value.userId }) {}
        assertTrue(importer.resume(owner))
        assertNull(local.get(null, id)); assertNotNull(local.get(owner, id))
        val remote = RestFormSource(FirestoreRest("demo-formlink", auth, FirebaseHttp(true), true))
        assertEquals(SyncResult.SUCCESS, FormSyncEngine(local, remote) { auth.state.value.userId }.sync(owner))
        assertTrue(remote.fetch(owner).any { it.id == id })
        assertNull(db.cloud().publication(owner, id))
        auth.signOut()
    }

    @Test fun existingAccountCanRestorePendingImportAfterAnotherAccountSignsIn() = runBlocking {
        val email = "existing-${UUID.randomUUID()}@example.com"
        var auth = auth()
        auth.state.first { it !is AuthState.Initializing }
        auth.signUp(email, "password123")
        val owner = checkNotNull(auth.state.value.userId)
        local.save(owner, draft("owned").copy(ownerId = owner))
        auth.signOut()
        local.save(null, draft("guest"))
        auth.signIn(email, "password123")
        assertEquals(GuestImportCounts(1, 0), store.observe(owner).first())
        auth.signOut()
        auth.signUp("other-${UUID.randomUUID()}@example.com", "password123")
        val other = checkNotNull(auth.state.value.userId)
        assertNotEquals(owner, other)
        assertTrue(GuestDraftImporter(store, { auth.state.value.userId }) {}.resume(other))
        assertNull(local.get(other, "guest")); assertNull(local.get(other, "owned"))
        auth.signOut()
        auth.signIn(email, "password123")
        auth = auth() // Recreate repository from the encrypted session without another login.
        assertEquals(owner, auth.state.first { it !is AuthState.Initializing }.userId)
        local.save(null, draft("later-guest"))
        assertTrue(GuestDraftImporter(store, { auth.state.value.userId }) {}.resume(owner))
        assertNotNull(local.get(owner, "guest")); assertNotNull(local.get(owner, "owned"))
        assertNotNull(local.get(null, "later-guest"))
        auth.signOut()
        auth.signIn(email, "password123")
        assertTrue(GuestDraftImporter(store, { auth.state.value.userId }) {}.resume(owner))
        assertNotNull(local.get(owner, "later-guest"))
        assertEquals(GuestImportCounts(0, 2), store.observe(owner).first())
        auth.signOut()
    }

    @Test fun interruptedSessionPersistenceKeepsTheConfirmedOwnersReservationForNextLogin() = runBlocking {
        local.save(null, draft("reserved"))
        val prepared = CompletableDeferred<String>()
        val hold = CompletableDeferred<Unit>()
        val delayed = object : GuestImportStore by store {
            override suspend fun prepare(owner: String, ids: Set<String>) {
                store.prepare(owner, ids)
                prepared.complete(owner)
                hold.await()
            }
        }
        val email = "interrupted-${UUID.randomUUID()}@example.com"
        val first = auth(delayed)
        first.state.first { it !is AuthState.Initializing }
        val login = launch { first.signUp(email, "password123") }
        val owner = withTimeout(30_000) { prepared.await() }
        login.cancelAndJoin()
        assertEquals(AuthState.Guest, first.state.value)
        assertNull(DeviceCipher(context).read("auth-session"))
        assertNotNull(local.get(null, "reserved"))
        assertTrue(store.snapshotGuestIds().isEmpty())
        val next = auth()
        next.state.first { it !is AuthState.Initializing }
        next.signIn(email, "password123")
        assertEquals(owner, next.state.value.userId)
        assertTrue(GuestDraftImporter(store, { next.state.value.userId }) {}.resume(owner))
        assertNotNull(local.get(owner, "reserved"))
        next.signOut()
    }
}
