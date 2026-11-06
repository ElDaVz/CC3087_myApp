package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.*
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class PublishedRequestFlowTest {
    @Test fun publishPublicSubmitSynchronizeReopenAndArchivePreservesSnapshots() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val http = FirebaseHttp(true)
        val auth = RestAuthRepository("demo-api-key", DeviceCipher(context), http, true)
        auth.state.first { it !is AuthState.Initializing }; auth.signOut()
        auth.signUp("publish-${UUID.randomUUID()}@example.com", "password123")
        val owner = auth.state.value.userId!!
        val client = FirestoreRest("demo-formlink", auth, http, true)
        val source = RestFormSource(client)
        val database = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        try {
            val local = RoomFormStore(database)
            val id = "published-${UUID.randomUUID()}"
            local.save(owner, FormDraft(id, "Publicado", listOf(FormFieldDraft("q", FormFieldType.TEXT, "Pregunta", true)), 1, 1, ownerId = owner))
            val form = local.get(owner, id)!!
            val publisher = RestFormPublisher(client, source, database, { auth.state.value.userId }, { it() })
            val publication = publisher.publish(owner, form)
            assertTrue(publication.url.endsWith("/f/$id"))
            assertEquals(publication.version, publisher.publish(owner, form).version)
            val p = FirestoreRest.fields(client.get(owner, "publishedForms/$id")!!)
            val attempt = UUID.randomUUID().toString()
            val answers = listOf(mapOf("fieldId" to "q", "value" to "Respuesta web"))
            val writes = mutableListOf<JSONObject>()
            for (part in 0..1) writes += client.update("forms/$id/responseChecks/${attempt}_$part", mapOf("attemptId" to attempt, "part" to part, "answers" to answers))
            writes += client.update("forms/$id/responses/$attempt", mapOf("ownerId" to owner, "formId" to id, "attemptId" to attempt,
                "title" to p["title"], "fields" to p["fields"], "version" to p["version"], "status" to "PENDING", "answers" to answers), listOf("submittedAt"))
            writes += client.update("publishedForms/$id/receipts/$attempt", mapOf("responseId" to attempt, "formId" to id, "version" to p["version"]), listOf("submittedAt"))
            http.request("http://10.0.2.2:8080/v1/projects/demo-formlink/databases/(default)/documents:commit", JSONObject().put("writes", JSONArray(writes)))
            val requests = CloudRequestRepository(database, client, owner, { auth.state.value.userId }) {}
            requests.sync()
            assertEquals(1, requests.getRequests().size)
            requests.updateStatus(id, attempt, RequestStatus.COMPLETED)
            requests.sync(); assertEquals(RequestStatus.COMPLETED, requests.getDetail(id, attempt)!!.requestStatus)
            requests.updateStatus(id, attempt, RequestStatus.PENDING); requests.sync()
            local.save(owner, form.copy(title = "Edición privada"))
            assertEquals("Publicado", FirestoreRest.fields(client.get(owner, "publishedForms/$id")!!)["title"])
            publisher.archive(owner, id)
            assertTrue(runCatching { http.request("http://10.0.2.2:8080/v1/projects/demo-formlink/databases/(default)/documents/publishedForms/$id") }.isFailure)
            assertEquals("Publicado", requests.getDetail(id, attempt)!!.formTitle)
            auth.signOut(); assertTrue(runCatching { requests.getDetail(id, attempt) }.isFailure)
        } finally { database.close(); auth.signOut() }
    }
}
