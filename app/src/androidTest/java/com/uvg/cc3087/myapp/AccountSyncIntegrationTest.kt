package com.uvg.cc3087.myapp

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.*
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class AccountSyncIntegrationTest {
    @Test fun importedDraftDeletionAndResponseFailureUseWholeAccountResult() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATORS && InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            private val folder = File(cacheDir, "account-sync-${UUID.randomUUID()}").apply { mkdirs() }
            override fun getNoBackupFilesDir() = folder
        }
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        val imports = RoomGuestImportStore(db)
        val http = FirebaseHttp(true)
        val auth = RestAuthRepository("demo-api-key", DeviceCipher(context), http, true, guestImports = imports)
        try {
            val local = RoomFormStore(db)
            val id = "account-${UUID.randomUUID()}"
            local.save(null, FormDraft(id, "Private imported draft", emptyList(), 1, 1))
            local.save(null, FormDraft("$id-gone", "Deleted guest", emptyList(), 1, 1))
            local.delete(null, "$id-gone")
            auth.state.first { it !is AuthState.Initializing }
            auth.signUp("$id@example.com", "password123")
            val owner = checkNotNull(auth.state.value.userId)
            assertTrue(GuestDraftImporter(imports, { auth.state.value.userId }, {}).resume(owner))
            assertNull(local.get(owner, "$id-gone"))
            val client = FirestoreRest("demo-formlink", auth, http, true)
            val engine = FormSyncEngine(local, RestFormSource(client)) { auth.state.value.userId }
            val publisher = RestFormPublisher(client, RestFormSource(client), db, { auth.state.value.userId }, { block -> engine.exclusive(block) })
            val requests = CloudRequestRepository(db, client, owner, { auth.state.value.userId }, {})
            val pending = AccountPendingStore(db)
            val online = MutableStateFlow(true)
            val pass = AccountSyncPass({ auth.state.value.userId },
                forms = engine::sync, publications = publisher::refresh, responses = { requests.sync() })
            val account = DefaultAccountSyncRepository(online, pending::observe, pending::snapshot,
                { auth.state.value.userId }, {}, synchronize = pass::run)
            assertEquals(AccountPendingChanges(drafts = 1), pending.snapshot(owner))
            assertEquals(SyncResult.SUCCESS, account.run(owner))
            assertNotNull(client.get(owner, "forms/$id"))
            assertNull(client.get(owner, "publishedForms/$id"))
            local.delete(owner, id)
            assertEquals(AccountPendingChanges(deletions = 1), pending.snapshot(owner))
            online.value = false
            assertEquals(SyncResult.RETRY, account.run(owner))
            assertEquals(AccountSyncPhase.WAITING_CONNECTION, account.observe(owner).first().phase)
            online.value = true
            assertEquals(SyncResult.SUCCESS, account.run(owner))
            assertEquals(true, FirestoreRest.fields(checkNotNull(client.get(owner, "forms/$id")))["deleted"])
            // A status write for a missing remote response is rejected by Firestore rules.
            db.cloud().saveRequest(RemoteRequestEntity(owner, id, "missing", "Snapshot", 1, "COMPLETED", "[]", 1, 0))
            assertEquals(AccountPendingChanges(responseStatuses = 1), pending.snapshot(owner))
            val last = account.observe(owner).first().lastSuccessfulCheck
            assertEquals(SyncResult.RETRY, account.run(owner))
            assertEquals(AccountSyncPhase.ERROR, account.observe(owner).first().phase)
            assertEquals(last, account.observe(owner).first().lastSuccessfulCheck)
            assertEquals(1, pending.snapshot(owner).responseStatuses)
        } finally { auth.signOut(); db.close(); context.noBackupFilesDir.deleteRecursively() }
    }
}
