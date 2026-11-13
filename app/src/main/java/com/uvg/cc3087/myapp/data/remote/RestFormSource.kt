package com.uvg.cc3087.myapp.data.remote

import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.RemoteFormSource
import com.uvg.cc3087.myapp.data.remote.http.FirestoreRest
import org.json.JSONObject
import java.time.Instant

class RestFormSource(private val client: FirestoreRest) : RemoteFormSource {
    override suspend fun fetch(owner: String) = client.query(owner, "forms").filter { FirestoreRest.fields(it)["deleted"] != true }.map { doc ->
        val row = FirestoreRest.fields(doc)
        check(row["ownerId"] == owner)
        @Suppress("UNCHECKED_CAST")
        FormDraft(doc.getString("name").substringAfterLast('/'), row["title"] as String,
            (row["fields"] as List<Map<*, *>>).sortedBy { (it["position"] as Number).toInt() }.map(FormFieldWire::decode),
            (row["createdAt"] as Instant).toEpochMilli(), (row["updatedAt"] as Instant).toEpochMilli(), ownerId = owner,
            creationAttemptId = row["creationAttemptId"] as? String,
            creationStartedAt = (row["creationStartedAt"] as? Instant)?.toEpochMilli(), templateType = row["templateType"] as? String)
    }
    override suspend fun put(owner: String, form: FormDraft) {
        require(form.ownerId == owner)
        val existing = client.get(owner, "forms/${form.id}")
        // A stale device must acknowledge its edit and then discard its local copy on fetch.
        if (existing?.let(FirestoreRest::fields)?.get("deleted") == true) return
        val values = linkedMapOf<String, Any?>("ownerId" to owner, "title" to form.title,
            "fields" to form.fields.mapIndexed { index, field -> FormFieldWire.encode(field, index) },
            "createdAt" to Instant.ofEpochMilli(form.createdAt), "version" to ((existing?.let(FirestoreRest::fields)?.get("version") as? Number)?.toLong() ?: 0) + 1)
        values["creationAttemptId"] = form.creationAttemptId
        values["creationStartedAt"] = form.creationStartedAt?.let(Instant::ofEpochMilli)
        values["templateType"] = form.templateType
        if (existing == null) { values["status"] = "DRAFT"; values["publishedAt"] = null }
        client.commit(owner, listOf(client.update("forms/${form.id}", values, listOf("updatedAt"),
            existing?.let { JSONObject().put("updateTime", it.getString("updateTime")) } ?: JSONObject().put("exists", false))))
    }
    override suspend fun delete(owner: String, id: String) {
        val form = client.get(owner, "forms/$id") ?: return
        val published = client.get(owner, "publishedForms/$id")
        val precondition = JSONObject().put("updateTime", form.getString("updateTime"))
        // Unpublished drafts need the same durable tombstone as published forms:
        // a second device may still have a pending edit for this document ID.
        val writes = mutableListOf(client.update("forms/$id",
            mapOf("deleted" to true, "status" to "ARCHIVED"), listOf("updatedAt"), precondition))
        if (published != null) writes += client.update("publishedForms/$id", mapOf("active" to false),
            precondition = JSONObject().put("updateTime", published.getString("updateTime")))
        client.commit(owner, writes)
    }
}
