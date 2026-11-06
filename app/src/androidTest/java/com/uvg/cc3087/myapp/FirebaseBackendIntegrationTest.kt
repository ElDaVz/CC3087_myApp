package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.RestFormSource
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class FirebaseBackendIntegrationTest {
    @Test fun realFirebaseTransportRecoversIntoFreshRoomAndRetainsOfflineEdits() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unique = UUID.randomUUID().toString()
        val auth = RestAuthRepository("demo-api-key", DeviceCipher(context), FirebaseHttp(true), true)
        auth.state.first { it !is AuthState.Initializing }; auth.signOut()
        val client = FirestoreRest("demo-formlink", auth, FirebaseHttp(true), true)
        val remote = RestFormSource(client)
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        val fresh = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val email = "owner-$unique@example.com"
            auth.signUp(email, "password123")
            val uid = auth.state.value.userId!!
            val local = RoomFormStore(db)
            val coordinator = OfflineFirstFormRepository(local, auth, uid) {}
            val form = FormDraft("integration-$unique", "Pedido remoto", listOf(
                FormFieldDraft("q", FormFieldType.TEXT, "Nombre", required = true)), 1, 1, "covers/device.jpg")
            coordinator.saveDraft(form)
            assertEquals(SyncResult.SUCCESS, FormSyncEngine(local, remote) { auth.state.value.userId }.sync(uid))
            assertTrue(local.pending(uid).isEmpty())
            coordinator.saveDraft(form.copy(title = "Pedido sin conexión"))
            val unavailable = object : RemoteFormSource by remote {
                override suspend fun put(owner: String, form: FormDraft) { throw java.io.IOException("Simulated network unavailable") }
            }
            assertEquals(SyncResult.RETRY, FormSyncEngine(local, unavailable) { auth.state.value.userId }.sync(uid))
            assertEquals("Pedido sin conexión", coordinator.getDraft(form.id)!!.title)
            assertEquals(1, local.pending(uid).size)
            assertEquals(SyncResult.SUCCESS, FormSyncEngine(local, remote) { auth.state.value.userId }.sync(uid))
            val document = FirestoreRest.fields(client.get(uid, "forms/${form.id}")!!)
            assertEquals(uid, document["ownerId"]); assertFalse(document.containsKey("coverPath"))
            auth.signOut(); assertTrue(runCatching { coordinator.getDraft(form.id) }.isFailure)
            assertTrue(runCatching { auth.signIn(email, "wrong-password") }.isFailure)
            auth.signIn(email, "password123")
            val restored = RoomFormStore(fresh)
            assertEquals(SyncResult.SUCCESS, FormSyncEngine(restored, remote) { auth.state.value.userId }.sync(uid))
            assertEquals("Pedido sin conexión", restored.get(uid, form.id)!!.title)
            assertNull(restored.get(uid, form.id)!!.coverPath)
            auth.signOut(); auth.signUp("other-$unique@example.com", "password123")
            assertTrue(runCatching { client.get(auth.state.value.userId!!, "forms/${form.id}") }.isFailure)
            assertNull(restored.get(auth.state.value.userId!!, form.id))
        } finally { auth.signOut(); db.close(); fresh.close() }
    }
}
