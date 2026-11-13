package com.uvg.cc3087.myapp

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.local.RoomFormStore
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.RestFormSource
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.RestAuthRepository
import com.uvg.cc3087.myapp.data.repository.FormSyncEngine
import com.uvg.cc3087.myapp.data.repository.SyncResult
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import java.io.File

class RestBackendIntegrationTest {
    // Each test owns its encrypted session; never replace the installed app's session.
    private val context by lazy {
        object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            private val sessionDirectory = File(cacheDir, "rest-auth-test-${UUID.randomUUID()}").apply { mkdirs() }
            override fun getNoBackupFilesDir(): File = sessionDirectory
        }
    }

    @After fun removeTestSession() {
        context.noBackupFilesDir.deleteRecursively()
    }

    @Test fun unpublishedDeletionOnDeviceABlocksPendingEditFromDeviceB() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val auth = RestAuthRepository("demo-api-key", DeviceCipher(context), FirebaseHttp(true), true)
        auth.state.first { it !is AuthState.Initializing }
        val databaseA = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        val databaseB = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            auth.signUp("two-devices-${UUID.randomUUID()}@example.com", "password123")
            val owner = auth.state.value.userId!!
            val client = FirestoreRest("demo-formlink", auth, FirebaseHttp(true), true)
            val source = RestFormSource(client)
            val deviceA = RoomFormStore(databaseA)
            val deviceB = RoomFormStore(databaseB)
            val syncA = FormSyncEngine(deviceA, source) { auth.state.value.userId }
            val syncB = FormSyncEngine(deviceB, source) { auth.state.value.userId }
            val draft = FormDraft("unpublished-${UUID.randomUUID()}", "Original draft", emptyList(), 1, 1, ownerId = owner)
            deviceA.save(owner, draft)
            assertEquals(SyncResult.SUCCESS, syncA.sync(owner))
            assertEquals(SyncResult.SUCCESS, syncB.sync(owner))
            val staleEdit = deviceB.get(owner, draft.id)!!.copy(title = "Pending edit on B")
            deviceB.save(owner, staleEdit)
            assertTrue(deviceB.pending(owner).isNotEmpty())
            deviceA.delete(owner, draft.id)
            assertEquals(SyncResult.SUCCESS, syncA.sync(owner))
            assertEquals(true, FirestoreRest.fields(client.get(owner, "forms/${draft.id}")!!)["deleted"])
            assertNull(client.get(owner, "publishedForms/${draft.id}"))
            // B reconnects with its old edit after A has confirmed deletion.
            assertEquals(SyncResult.SUCCESS, syncB.sync(owner))
            assertNull(deviceB.get(owner, draft.id))
            assertTrue(deviceB.pending(owner).isEmpty())
            assertTrue(source.fetch(owner).none { it.id == draft.id })
            assertEquals("Original draft", FirestoreRest.fields(client.get(owner, "forms/${draft.id}")!!)["title"])
            assertTrue(runCatching { deviceB.save(owner, staleEdit) }.isFailure)
            // Retrying either deletion must retain the remote marker.
            source.delete(owner, draft.id)
            assertEquals(true, FirestoreRest.fields(client.get(owner, "forms/${draft.id}")!!)["deleted"])
        } finally {
            databaseA.close()
            databaseB.close()
            auth.signOut()
        }
    }

    @Test fun passwordResetUsesEmulatorCodeWithoutStartingSession() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val http = FirebaseHttp(true)
        val auth = RestAuthRepository("demo-api-key", DeviceCipher(context), http, true)
        auth.state.first { it !is AuthState.Initializing }; auth.signOut()
        val email = "reset-${UUID.randomUUID()}@example.com"
        try {
            auth.signUp(email, "password123")
            val uid = auth.state.value.userId
            auth.signOut()
            auth.sendPasswordReset("  $email  ")
            assertEquals(AuthState.Guest, auth.state.value)
            val codes = http.request("http://10.0.2.2:9099/emulator/v1/projects/demo-formlink/oobCodes").getJSONArray("oobCodes")
            val code = (0 until codes.length()).map { codes.getJSONObject(it) }.single { it.getString("email") == email }
            assertEquals("PASSWORD_RESET", code.getString("requestType"))
            http.request("http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:resetPassword?key=demo-api-key",
                org.json.JSONObject().put("oobCode", code.getString("oobCode")).put("newPassword", "replacement123"))
            assertTrue(runCatching { auth.signIn(email, "password123") }.isFailure)
            auth.signIn(email, "replacement123")
            assertEquals(uid, auth.state.value.userId)
            auth.signOut()
            auth.sendPasswordReset("unknown-${UUID.randomUUID()}@example.com")
            assertEquals(AuthState.Guest, auth.state.value)
        } finally { auth.signOut() }
    }
    @Test fun missingOrDifferentProjectConfigDoesNotRestoreAnEncryptedSession() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val cipher=DeviceCipher(context)
        val auth=RestAuthRepository("demo-api-key",cipher,FirebaseHttp(true),true)
        auth.state.first { it !is AuthState.Initializing };auth.signOut()
        try {
            auth.signUp("config-${UUID.randomUUID()}@example.com","password123")
            val absent=RestAuthRepository(null,cipher,FirebaseHttp(true),true)
            assertEquals(AuthState.Guest,absent.state.first { it !is AuthState.Initializing })
            assertFalse(absent.configured)
            val other=RestAuthRepository("different-project-key",cipher,FirebaseHttp(true),true)
            assertEquals(AuthState.Guest,other.state.first { it !is AuthState.Initializing })
            assertNotNull(cipher.read("auth-session"))
        } finally { auth.signOut() }
    }
    @Test fun encryptedRestSessionCanSignOutSignInAndRecoverOwnerForm() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        var clock = System.currentTimeMillis()
        val auth = RestAuthRepository("demo-api-key", DeviceCipher(context), FirebaseHttp(true), true, now = { clock })
        auth.state.first { it !is AuthState.Initializing }
        auth.signOut()
        val email = "rest-${UUID.randomUUID()}@example.com"
        auth.signUp(email, "password123")
        val uid = auth.state.value.userId!!
        clock += 3_700_000
        assertTrue(auth.token(uid).isNotBlank())
        assertEquals(uid, auth.state.value.userId)
        val client = FirestoreRest("demo-formlink", auth, FirebaseHttp(true), true)
        val source = RestFormSource(client)
        val id = "rest-${UUID.randomUUID()}"
        source.put(uid, FormDraft(id, "REST persisted", listOf(FormFieldDraft("q", FormFieldType.TEXT, "Question")), 1, 1, ownerId = uid))
        assertEquals("REST persisted", source.fetch(uid).single { it.id == id }.title)
        val encrypted = java.io.File(context.noBackupFilesDir, "auth-session").readBytes()
        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains(email))
        auth.signOut()
        assertTrue(runCatching { source.fetch(uid) }.isFailure)
        auth.signIn(email, "password123")
        assertEquals(uid, auth.state.value.userId)
        assertEquals(id, source.fetch(uid).single().id)
        auth.signOut()
        auth.signUp("other-${UUID.randomUUID()}@example.com", "password123")
        assertTrue(runCatching { client.get(auth.state.value.userId!!, "forms/$id") }.isFailure)
        auth.signOut()
    }
}
