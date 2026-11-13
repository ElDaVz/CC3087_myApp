package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RequestReadRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun guestReadingSurvivesReopenStatusChangesAndRepeatedMarks() = runBlocking {
        val name = "reading-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, FormLinkDatabase::class.java, name).build()
        try {
            val db = open()
            try {
                db.requests().insert(RequestEntity("f", "r", "Snapshot", 1, "PENDING", "[]"))
                val repo = RoomRequestRepository(db)
                assertFalse(repo.observeRequests().first().single().isRead)
                repo.markRead("f", "r"); repo.markRead("f", "r")
                repo.updateStatus("f", "r", RequestStatus.COMPLETED)
                assertTrue(repo.observeRequests().first().single().isRead)
                assertEquals(1, db.requestReads().get("guest").size)
                assertTrue(runCatching { repo.markRead("f", "absent") }.isFailure)
            } finally { db.close() }
            val restored = open()
            try { assertTrue(RoomRequestRepository(restored).getRequests().single().isRead) }
            finally { restored.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun readingsAreIsolatedByAccountFormAndGuestAndSurviveRemoteUpsert() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        var active = "a"
        val client = FirestoreRest("demo-formlink", RestAuthRepository(null, DeviceCipher(context), FirebaseHttp(true), true), FirebaseHttp(true), true)
        val scheduled = mutableListOf<String>()
        fun repository(owner: String) = CloudRequestRepository(db, client, owner, { active }, { scheduled += it })
        try {
            for (owner in listOf("a", "b")) for (form in listOf("f", "other"))
                db.cloud().saveRequest(RemoteRequestEntity(owner, form, "same", "Title", 1, "PENDING", "[]"))
            db.requests().insert(RequestEntity("f", "same", "Guest", 1, "PENDING", "[]"))
            val a = repository("a")
            a.markRead("f", "same"); a.requestRefresh()
            assertEquals(listOf("a"), scheduled)
            assertTrue(a.getRequests().single { it.formId == "f" }.isRead)
            assertFalse(a.getRequests().single { it.formId == "other" }.isRead)
            assertFalse(RoomRequestRepository(db).getRequests().single().isRead)
            // Sync replaces the snapshot, not its separate reading history.
            db.cloud().saveRequest(RemoteRequestEntity("a", "f", "same", "Title", 1, "COMPLETED", "[]"))
            assertTrue(a.observeRequests().first().single { it.formId == "f" }.isRead)
            active = "b"
            assertFalse(repository("b").getRequests().any { it.isRead })
            assertTrue(runCatching { a.markRead("f", "same") }.exceptionOrNull() is CancellationException)
            active = "a"
            assertTrue(repository("a").getRequests().single { it.formId == "f" }.isRead)
        } finally { db.close() }
    }
}
