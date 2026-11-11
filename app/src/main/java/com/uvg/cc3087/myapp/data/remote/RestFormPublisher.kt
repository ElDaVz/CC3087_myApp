package com.uvg.cc3087.myapp.data.remote

import com.uvg.cc3087.myapp.domain.telemetry.firstPublished
import com.uvg.cc3087.myapp.data.model.CoverAsset
import androidx.room.withTransaction
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.remote.http.FirestoreRest
import com.uvg.cc3087.myapp.domain.validation.*
import org.json.JSONObject
import java.time.Instant
import java.security.MessageDigest

fun contentFingerprint(form: FormDraft, coverHash: String? = null): String = MessageDigest.getInstance("SHA-256")
    .digest((form.title + "\u0000" + form.fields.toString() + (coverHash?.let { "\u0000cover:$it" } ?: "")).toByteArray()).joinToString("") { "%02x".format(it) }

class RestFormPublisher(private val client: FirestoreRest, private val source: RemoteFormSource,
    private val database: FormLinkDatabase, private val currentOwner: () -> String?,
    private val exclusive: suspend (suspend () -> FormPublication) -> FormPublication,
    private val analytics: com.uvg.cc3087.myapp.domain.telemetry.ProductAnalytics = com.uvg.cc3087.myapp.domain.telemetry.NoProductAnalytics,
    private val covers: CoverStorage? = null, private val uploader: CoverUploader? = null) : FormPublisher {
    private fun checkSession(owner: String) { check(currentOwner() == owner) { "Session changed" } }
    private fun asset(row: PublicationEntity): CoverAsset? = row.coverUrl?.let {
        CoverAsset.fromWire(mapOf("url" to it, "publicId" to row.coverPublicId, "hash" to row.coverHash))
    }
    private fun needsUpload(form: FormDraft, previous: CoverAsset?) = form.coverAction != "REMOVE" &&
        form.coverPath != null && (form.coverAction == "REPLACE" || previous == null)
    private suspend fun prepare(form: FormDraft): PreparedCover {
        try { return checkNotNull(covers).prepareUpload(checkNotNull(form.coverPath)) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { throw PublicationException("No se pudo preparar la portada. Vuelve a seleccionarla y reintenta.") }
    }
    override suspend fun publication(owner: String, form: FormDraft): FormPublication? {
        val row = database.cloud().publication(owner, form.id) ?: return null
        val previous = asset(row)
        val hash = when {
            form.coverAction == "REMOVE" -> null
            needsUpload(form, previous) -> try { prepare(form).hash }
                catch (error: PublicationException) { return FormPublication(row.url, row.status, row.version, row.firstPublishedAt, false, row.coverUrl) }
            else -> previous?.hash
        }
        return FormPublication(row.url, row.status, row.version, row.firstPublishedAt,
            row.fingerprint == contentFingerprint(form, hash), row.coverUrl)
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
        val oldPublicDoc = client.get(owner, "publishedForms/${form.id}")
        val oldPublic = oldPublicDoc?.let(FirestoreRest::fields)
        val previous = CoverAsset.fromWire(oldPublic?.get("cover"))
        val selected = when {
            form.coverAction == "REMOVE" -> null
            needsUpload(form, previous) -> {
                val prepared = prepare(form)
                checkSession(owner)
                val cached = database.cloud().uploadedCover(owner, form.id, prepared.hash)
                if (cached != null) CoverAsset.fromWire(mapOf("url" to cached.url, "publicId" to cached.publicId, "hash" to cached.hash))
                else {
                    val uploaded = (uploader ?: throw PublicationException("Configura Cloudinary para publicar una portada")).upload(prepared)
                    checkSession(owner)
                    check(uploaded.hash == prepared.hash)
                    database.cloud().saveCoverUpload(CoverUploadEntity(owner, form.id, prepared.hash, uploaded.url, uploaded.publicId))
                    uploaded
                }
            }
            else -> previous
        }
        checkSession(owner)
        val fingerprint = contentFingerprint(form, selected?.hash)
        if (oldPublic?.get("fingerprint") != fingerprint || oldPublic["active"] != true) {
            val version = ((oldPublic?.get("version") as? Number)?.toLong() ?: 0L) + 1
            val fields = form.fields.mapIndexed { i, field -> FormFieldWire.encode(field, i) +
                mapOf("optionIds" to field.options.map { it.id }) }
            val publicValues = linkedMapOf<String, Any?>("ownerId" to owner, "title" to form.title,
                "fields" to fields, "active" to true, "version" to version, "fingerprint" to fingerprint,
                "templateType" to (form.templateType ?: "LEGACY"), "cover" to selected?.toWire())
            if (oldPublic == null) {
                publicValues["firstFieldCount"] = form.fields.size
                publicValues["firstTemplateType"] = form.templateType ?: "LEGACY"
            }
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
                    if (oldPublicDoc == null) JSONObject().put("exists", false) else JSONObject().put("updateTime", oldPublicDoc.getString("updateTime")))))
        }
        checkSession(owner)
        val confirmed = FirestoreRest.fields(checkNotNull(client.get(owner, "publishedForms/${form.id}")))
        val cover = CoverAsset.fromWire(confirmed["cover"])
        val row = PublicationEntity(form.id, owner, "PUBLISHED", "https://${client.project}.web.app/f/${form.id}",
            (confirmed["publishedAt"] as Instant).toEpochMilli(), (confirmed["version"] as Number).toLong(),
            confirmed["fingerprint"] as String, (confirmed["firstPublishedAt"] as Instant).toEpochMilli(), cover?.url, cover?.publicId, cover?.hash)
        database.cloud().savePublication(row)
        analytics.firstPublished(form, System.currentTimeMillis(),
            (confirmed["firstFieldCount"] as? Number)?.toInt() ?: form.fields.size,
            confirmed["firstTemplateType"] as? String ?: form.templateType ?: "LEGACY")
        FormPublication(row.url, row.status, row.version, row.firstPublishedAt, row.fingerprint == fingerprint, cover?.url)
    }
    suspend fun refresh(owner: String) {
        val docs = client.query(owner, "publishedForms")
        checkSession(owner)
        for (doc in docs) {
            val row = FirestoreRest.fields(doc)
            val cover = CoverAsset.fromWire(row["cover"])
            val id = doc.getString("name").substringAfterLast('/')
            database.cloud().savePublication(PublicationEntity(id, owner, if (row["active"] == true) "PUBLISHED" else "ARCHIVED",
                "https://${client.project}.web.app/f/$id", (row["publishedAt"] as Instant).toEpochMilli(),
                (row["version"] as Number).toLong(), row["fingerprint"] as String, (row["firstPublishedAt"] as Instant).toEpochMilli(), cover?.url, cover?.publicId, cover?.hash))
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
