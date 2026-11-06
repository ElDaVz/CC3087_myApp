package com.uvg.cc3087.myapp.data.repository

import androidx.room.withTransaction
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.http.FirestoreRest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Account-owned snapshots, with a durable Room outbox for status transitions. */
class CloudRequestRepository(private val db: FormLinkDatabase, private val client: FirestoreRest,
    private val owner: String, private val currentOwner: () -> String?, private val schedule: (String) -> Unit) : RequestRepository {
    private fun checkSession() { if (currentOwner() != owner) throw CancellationException("Session changed") }
    override fun observeRequests() = db.cloud().observeRequests(owner).map { rows -> checkSession(); rows.map { it.detail().toSummary() } }
    override suspend fun getRequests(): List<RequestSummary> { checkSession(); return db.cloud().requests(owner).map { it.detail().toSummary() } }
    override suspend fun getDetail(formId: String, responseId: String): RequestDetail? { checkSession(); return db.cloud().detail(owner, formId, responseId)?.detail() }
    override suspend fun updateStatus(formId: String, responseId: String, status: RequestStatus): RequestDetail {
        checkSession()
        val row = db.withTransaction {
            val old = checkNotNull(db.cloud().detail(owner, formId, responseId))
            old.copy(status = status.name, statusRevision = old.statusRevision + 1).also { db.cloud().saveRequest(it) }
        }
        schedule(owner)
        return row.detail()
    }
    suspend fun sync() {
        checkSession()
        for (row in db.cloud().requests(owner).filter { it.statusRevision != it.syncedStatusRevision }) {
            client.commit(owner, listOf(client.update("forms/${row.formId}/responses/${row.responseId}", mapOf("status" to row.status))))
            checkSession()
            db.cloud().acknowledge(owner, row.formId, row.responseId, row.statusRevision)
        }
        val documents = client.query(owner, "responses", allDescendants = true)
        checkSession()
        db.withTransaction {
            for (doc in documents) {
                val value = FirestoreRest.fields(doc)
                check(value["ownerId"] == owner)
                val formId = value["formId"] as String
                val responseId = doc.getString("name").substringAfterLast('/')
                val existing = db.cloud().detail(owner, formId, responseId)
                if (existing != null && existing.statusRevision != existing.syncedStatusRevision) continue
                @Suppress("UNCHECKED_CAST")
                val fields = value["fields"] as List<Map<String, Any?>>
                @Suppress("UNCHECKED_CAST")
                val rawAnswers = value["answers"] as List<Map<String, Any?>>
                val answers = fields.mapIndexed { index, field ->
                    val answer = rawAnswers[index]["value"]
                    val kind = when { field["type"] in listOf("MULTIPLE_CHOICE", "CHECKBOX_GROUP", "DROPDOWN") -> "choices"
                        field["type"] == "CHECKBOX" -> "checked"; else -> "text" }
                    val displayed: Any = when (kind) {
                        "choices" -> {
                            val config = field["config"] as Map<*, *>
                            val options = config["options"] as List<*>
                            JSONArray(options.map { it as Map<*, *> }.filter { it["id"] in (answer as List<*>) }.map { it["label"] })
                        }
                        "checked" -> answer as Boolean
                        else -> if (answer is Map<*, *>) {
                            val date = answer["date"] as? Instant
                            val minute = (answer["minute"] as? Number)?.toInt()
                            listOfNotNull(date?.toString()?.take(10), minute?.let { "%02d:%02d".format(it / 60, it % 60) }).joinToString(" ")
                        } else answer?.toString().orEmpty()
                    }
                    JSONObject().put("fieldId", field["id"]).put("label", field["label"]).put("kind", kind).put("value", displayed)
                }
                db.cloud().saveRequest(RemoteRequestEntity(owner, formId, responseId, value["title"] as String,
                    (value["submittedAt"] as Instant).toEpochMilli(), value["status"] as String, JSONArray(answers).toString(),
                    existing?.statusRevision ?: 0, existing?.syncedStatusRevision ?: 0))
            }
        }
    }
}
private fun RemoteRequestEntity.detail(): RequestDetail {
    val rows = JSONArray(answersJson)
    return RequestDetail(formId, responseId, formTitle, Instant.ofEpochMilli(submittedAt), (0 until rows.length()).map {
        val row = rows.getJSONObject(it)
        RequestDetailAnswer(row.getString("fieldId"), row.getString("label"), when (row.getString("kind")) {
            "choices" -> row.getJSONArray("value").let { values -> RequestAnswerValue.Choices((0 until values.length()).map(values::getString)) }
            "checked" -> RequestAnswerValue.Checked(row.getBoolean("value"))
            else -> RequestAnswerValue.Text(row.getString("value"))
        })
    }, requestStatus = RequestStatus.valueOf(status))
}
