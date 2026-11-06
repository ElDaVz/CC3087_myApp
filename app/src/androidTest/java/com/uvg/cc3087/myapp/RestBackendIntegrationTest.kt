package com.uvg.cc3087.myapp

import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.RestFormSource
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.RestAuthRepository
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class RestBackendIntegrationTest {
    @Test fun encryptedRestSessionCanSignOutSignInAndRecoverOwnerForm() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
