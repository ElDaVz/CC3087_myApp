package com.uvg.cc3087.myapp.data.remote

import androidx.room.withTransaction
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.remote.http.FirestoreRest
import com.uvg.cc3087.myapp.domain.validation.*
import org.json.JSONObject
import java.time.Instant
import java.security.MessageDigest

fun contentFingerprint(form: FormDraft): String = MessageDigest.getInstance("SHA-256")
    .digest((form.title + "\u0000" + form.fields.toString()).toByteArray()).joinToString("") { "%02x".format(it) }

class RestFormPublisher(private val client: FirestoreRest, private val source: RemoteFormSource,
    private val database: FormLinkDatabase, private val currentOwner: () -> String?,
    private val exclusive: suspend (suspend () -> FormPublication) -> FormPublication) : FormPublisher {
    private fun checkSession(owner: String) { check(currentOwner() == owner) { "Session changed" } }
    override suspend fun publication(owner: String, form: FormDraft): FormPublication? = database.cloud().publication(owner, form.id)?.let {
        FormPublication(it.url, it.status, it.version, it.firstPublishedAt, it.fingerprint == contentFingerprint(form))
    }
    override suspend fun publish(owner: String, form: FormDraft): FormPublication = exclusive {
        checkSession(owner)
        if (FormDraftValidator.validate(form.title, form.fields) != FormValidationResult.READY)
            throw PublicationException("Corrige los errores del formulario antes de publicar")
        if (form.fields.size > 10) throw PublicationException("La web inicial admite hasta 10 preguntas")
        if (form.title.length > 200 || form.fields.any { it.title.length > 200 || it.options.any { option -> option.label.length > 200 } })
            throw PublicationException("Los títulos y opciones admiten hasta 200 caracteres")
        source.put(owner, form)
        checkSession(owner)
        val privateDoc = checkNotNull(client.get(owner, "forms/${form.id}"))
        val remote = FirestoreRest.fields(privateDoc)
        val fingerprint = contentFingerprint(form)
        val oldPublic = client.get(owner, "publishedForms/${form.id}")?.let(FirestoreRest::fields)
        if (oldPublic?.get("fingerprint") != fingerprint || oldPublic["active"] != true) {
            val version = ((oldPublic?.get("version") as? Number)?.toLong() ?: 0L) + 1
            val fields = form.fields.mapIndexed { i, field -> FormFieldWire.encode(field, i) +
                mapOf("optionIds" to field.options.map { it.id }) }
            val publicValues = linkedMapOf<String, Any?>("ownerId" to owner, "title" to form.title,
                "fields" to fields, "active" to true, "version" to version, "fingerprint" to fingerprint,
                "templateType" to (form.templateType ?: "LEGACY"))
            val times = mutableListOf("publishedAt")
            if (oldPublic?.get("firstPublishedAt") == null) times += "firstPublishedAt"
            val privateValues = mutableMapOf<String, Any?>("status" to "PUBLISHED", "publishedVersion" to version,
                "publishedFingerprint" to fingerprint)
            val privateTimes = mutableListOf("publishedAt", "updatedAt")
            if (remote["firstPublishedAt"] == null) privateTimes += "firstPublishedAt"
            check(database.drafts().getDraft(form.id)?.toDraft() == form) { "El formulario cambió; vuelve a abrir Publicar" }
            client.commit(owner, listOf(
                client.update("forms/${form.id}", privateValues, privateTimes, JSONObject().put("updateTime", privateDoc.getString("updateTime"))),
                client.update("publishedForms/${form.id}", publicValues, times,
                    if (oldPublic == null) JSONObject().put("exists", false) else null)))
        }
        checkSession(owner)
        val confirmed = FirestoreRest.fields(checkNotNull(client.get(owner, "publishedForms/${form.id}")))
        val row = PublicationEntity(form.id, owner, "PUBLISHED", "https://${client.project}.web.app/f/${form.id}",
            (confirmed["publishedAt"] as Instant).toEpochMilli(), (confirmed["version"] as Number).toLong(),
            confirmed["fingerprint"] as String, (confirmed["firstPublishedAt"] as Instant).toEpochMilli())
        database.cloud().savePublication(row)
        FormPublication(row.url, row.status, row.version, row.firstPublishedAt, row.fingerprint == fingerprint)
    }
    suspend fun refresh(owner: String) {
        val docs = client.query(owner, "publishedForms")
        checkSession(owner)
        for (doc in docs) {
            val row = FirestoreRest.fields(doc)
            val id = doc.getString("name").substringAfterLast('/')
            database.cloud().savePublication(PublicationEntity(id, owner, if (row["active"] == true) "PUBLISHED" else "ARCHIVED",
                "https://${client.project}.web.app/f/$id", (row["publishedAt"] as Instant).toEpochMilli(),
                (row["version"] as Number).toLong(), row["fingerprint"] as String, (row["firstPublishedAt"] as Instant).toEpochMilli()))
        }
    }
    override suspend fun archive(owner: String, formId: String) {
        checkSession(owner)
        val public = client.get(owner, "publishedForms/$formId") ?: return
        client.commit(owner, listOf(client.update("forms/$formId", mapOf("status" to "ARCHIVED"), listOf("updatedAt")),
            client.update("publishedForms/$formId", mapOf("active" to false), precondition = JSONObject().put("updateTime", public.getString("updateTime")))))
        checkSession(owner)
        database.cloud().publication(owner, formId)?.let { database.cloud().savePublication(it.copy(status = "ARCHIVED")) }
    }
}
