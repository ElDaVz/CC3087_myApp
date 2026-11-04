package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.FirestoreFormSource
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class FirebaseBackendIntegrationTest {
    @Test fun realFirebaseSdksAuthenticateAndRecoverFormsIntoAFreshRoomDatabase() = runBlocking {
        assumeTrue("Run with -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true and Firebase Emulator Suite",
            InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unique = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(context, FirebaseOptions.Builder().setProjectId("demo-formlink")
            .setApplicationId("1:1234567890:android:integration").setApiKey("demo-api-key").build(), "integration-$unique")
        val firebaseAuth = FirebaseAuth.getInstance(app).apply { useEmulator("10.0.2.2", 9099) }
        val firestore = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
            useEmulator("10.0.2.2", 8080)
        }
        val auth = FirebaseAuthRepository(firebaseAuth)
        val remote = FirestoreFormSource(firestore)
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        val fresh = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val email = "owner-$unique@example.com"
            auth.signUp(email, "password123")
            val signedIn = auth.state.first { it is AuthState.SignedIn } as AuthState.SignedIn
            val uid = signedIn.session.userId
            val local = RoomFormStore(db)
            val coordinator = OfflineFirstFormRepository(local, auth, uid) {}
            val form = FormDraft("integration-$unique", "Pedido remoto", listOf(
                FormFieldDraft("q", FormFieldType.TEXT, "Nombre", required = true)), 1, 1, "covers/device.jpg")
            coordinator.saveDraft(form)
            assertEquals(SyncResult.SUCCESS, FormSyncEngine(local, remote) { firebaseAuth.currentUser?.uid }.sync(uid))
            assertTrue(local.pending(uid).isEmpty())
            firestore.disableNetwork().await()
            coordinator.saveDraft(form.copy(title = "Pedido sin conexión"))
            assertEquals("Pedido sin conexión", coordinator.getDraft(form.id)!!.title)
            assertEquals(SyncResult.RETRY, FormSyncEngine(local, remote, timeoutMillis = 500) { firebaseAuth.currentUser?.uid }.sync(uid))
            assertEquals(1, local.pending(uid).size)
            firestore.enableNetwork().await()
            assertEquals(SyncResult.SUCCESS, FormSyncEngine(local, remote) { firebaseAuth.currentUser?.uid }.sync(uid))
            assertEquals("Pedido sin conexión", remote.fetch(uid).single().title)
            coordinator.saveDraft(form)
            assertEquals(SyncResult.SUCCESS, FormSyncEngine(local, remote) { firebaseAuth.currentUser?.uid }.sync(uid))
            val document = firestore.collection("forms").document(form.id).get(Source.SERVER).await()
            assertEquals(uid, document.getString("ownerId"))
            assertFalse(document.contains("coverPath"))
            auth.signOut()
            auth.state.first { it is AuthState.Guest }
            assertTrue(runCatching { coordinator.getDraft(form.id) }.isFailure)
            assertTrue(runCatching { auth.signIn(email, "wrong-password") }.isFailure)
            auth.signIn(email, "password123")
            auth.state.first { it is AuthState.SignedIn }
            val restored = RoomFormStore(fresh)
            assertEquals(SyncResult.SUCCESS, FormSyncEngine(restored, remote) { firebaseAuth.currentUser?.uid }.sync(uid))
            assertEquals("Pedido remoto", restored.get(uid, form.id)!!.title)
            assertEquals(form.fields, restored.get(uid, form.id)!!.fields)
            assertNull(restored.get(uid, form.id)!!.coverPath)
            auth.signOut(); auth.state.first { it is AuthState.Guest }
            auth.signUp("other-$unique@example.com", "password123")
            auth.state.first { it is AuthState.SignedIn }
            assertTrue(runCatching { firestore.collection("forms").document(form.id).get(Source.SERVER).await() }.isFailure)
            assertNull(restored.get(firebaseAuth.currentUser!!.uid, form.id))
        } finally {
            firebaseAuth.signOut(); db.close(); fresh.close(); firestore.terminate().await(); app.delete()
        }
    }
}
