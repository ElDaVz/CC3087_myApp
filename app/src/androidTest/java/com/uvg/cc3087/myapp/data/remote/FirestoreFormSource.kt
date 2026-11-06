package com.uvg.cc3087.myapp.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.RemoteFormSource
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreFormSource(private val firestore: FirebaseFirestore) : RemoteFormSource {
    override suspend fun fetch(owner: String): List<FormDraft> {
        // Never reconcile deletions against an incomplete/offline cached result.
        val snapshot = firestore.collection("forms").whereEqualTo("ownerId", owner).get(Source.SERVER).await()
        return snapshot.documents.map { document ->
            check(document.getString("ownerId") == owner)
            check(document.getString("status") == "DRAFT") { "Unsupported form visibility" }
            val fields = (document.get("fields") as? List<*>)?.map {
                it as? Map<*, *> ?: error("Invalid remote field")
            }?.sortedBy { (it["position"] as? Number)?.toInt() ?: error("Missing field position") }
                ?.map(FormFieldWire::decode) ?: error("Missing fields")
            require(fields.map { it.id }.distinct().size == fields.size)
            FormDraft(document.id, document.getString("title") ?: error("Missing title"), fields,
                document.getTimestamp("createdAt")?.toDate()?.time ?: error("Missing creation time"),
                document.getTimestamp("updatedAt")?.toDate()?.time ?: error("Missing update time"), ownerId = owner)
        }
    }
    override suspend fun put(owner: String, form: FormDraft) {
        require(form.ownerId == owner)
        firestore.collection("forms").document(form.id).set(mapOf(
            "ownerId" to owner, "title" to form.title, "status" to "DRAFT",
            "fields" to form.fields.mapIndexed { index, field -> FormFieldWire.encode(field, index) },
            "createdAt" to Timestamp(Date(form.createdAt)), "updatedAt" to FieldValue.serverTimestamp(),
            "publishedAt" to null, "version" to FieldValue.increment(1)
        )).await()
    }
    override suspend fun delete(owner: String, id: String) {
        // Owner access is enforced by Firestore rules, including deletes.
        firestore.collection("forms").document(id).delete().await()
    }
}
