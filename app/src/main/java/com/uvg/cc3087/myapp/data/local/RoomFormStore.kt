package com.uvg.cc3087.myapp.data.local

import androidx.room.withTransaction
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.flow.combine

/** Every access is scoped, including direct reads by ID and background acknowledgements. */
class RoomFormStore(private val database: FormLinkDatabase, private val covers: CoverStorage? = null) : LocalFormStore {
    private val dao get() = database.drafts()
    override fun observe(owner: String?) = combine(dao.observeOwned(owner), database.requests().observeCounts(),
        database.cloud().observeRequests(owner.orEmpty()), database.cloud().observePublications(owner.orEmpty())) { rows, counts, remoteRequests, publications ->
        val byId = if (owner == null) counts.associate { it.formId to it.count } else remoteRequests.groupingBy { it.formId }.eachCount()
        val publishedById = publications.associateBy { it.formId }
        val active = publications.filter { it.status == "PUBLISHED" }.map { it.formId }.toSet()
        rows.map { it.toDraft().toSummary().copy(responseCount = byId[it.form.id] ?: 0,
            publishedCoverUrl = publishedById[it.form.id]?.coverUrl,
            status = if (it.form.id in active) com.uvg.cc3087.myapp.data.model.FormStatus.ACTIVE else com.uvg.cc3087.myapp.data.model.FormStatus.DRAFT) }
    }
    override fun observePublishedCoverUrl(owner: String?, id: String) =
        if (owner == null) kotlinx.coroutines.flow.flowOf(null) else database.cloud().observePublishedCoverUrl(owner, id)
    override fun pendingCount(owner: String) = dao.observePendingCount(owner)
    override suspend fun get(owner: String?, id: String) = dao.getDraft(id)?.takeIf { it.form.ownerId == owner && !it.form.deleted }?.toDraft()

    override suspend fun save(owner: String?, draft: FormDraft) {
        require(draft.coverAction in setOf("KEEP", "REPLACE", "REMOVE"))
        require(draft.id.isNotBlank() && draft.fields.map { it.id }.distinct().size == draft.fields.size)
        require(draft.ownerId == null || draft.ownerId == owner)
        val oldCover = database.withTransaction {
            val old = dao.getDraft(draft.id)?.form
            check(old == null || (old.ownerId == owner && !old.deleted)) { "Form is outside this session" }
            val entity = draft.copy(ownerId = owner).toEntity().copy(
                revision = (old?.revision ?: 0) + 1, syncedRevision = old?.syncedRevision ?: 0)
            dao.saveDraft(entity, draft.toFieldEntities())
            old?.coverPath?.takeIf { it != draft.coverPath }
        }
        oldCover?.let { cleanup(it) }
    }
    override suspend fun delete(owner: String?, id: String) {
        val cover = database.withTransaction {
            val old = dao.getDraft(id) ?: return@withTransaction null
            check(old.form.ownerId == owner) { "Form is outside this session" }
            if (owner == null) dao.deleteDraft(id)
            else dao.saveDraft(old.form.copy(deleted = true, revision = old.form.revision + 1), old.fields)
            old.form.coverPath
        }
        cover?.let { cleanup(it) }
    }
    override suspend fun pending(owner: String) = dao.owned(owner).filter { it.form.revision != it.form.syncedRevision }
        .map { PendingForm(it.toDraft(), it.form.revision, it.form.deleted) }
    override suspend fun acknowledge(owner: String, pending: PendingForm) {
        database.withTransaction {
            if (pending.deleted) dao.acknowledgeDeletion(owner, pending.draft.id, pending.revision)
            else dao.acknowledge(owner, pending.draft.id, pending.revision)
        }
        if (pending.deleted) pending.draft.coverPath?.let { cleanup(it) }
    }
    override suspend fun importGuests(owner: String, ids: Set<String>) = database.withTransaction {
        for (id in ids) {
            val row = dao.getDraft(id) ?: error("Guest draft disappeared")
            check(row.form.ownerId == null && !row.form.deleted) { "Only guest drafts may be imported" }
            dao.saveDraft(row.form.copy(ownerId = owner, revision = row.form.revision + 1, syncedRevision = 0), row.fields)
        }
    }
    override suspend fun mergeRemote(owner: String, forms: List<FormDraft>) {
        val removed = database.withTransaction {
            val local = dao.owned(owner).associateBy { it.form.id }
            val paths = mutableListOf<String>()
            for (remote in forms) {
                require(remote.ownerId == owner)
                val existing = dao.getDraft(remote.id)
                check(existing == null || existing.form.ownerId == owner) { "Remote form ID belongs to another partition" }
                if (existing != null && existing.form.revision != existing.form.syncedRevision) continue
                // Cover files are device-local and never overwritten by a remote download.
                val copy = remote.copy(coverPath = existing?.form?.coverPath)
                dao.saveDraft(copy.toEntity().copy(revision = existing?.form?.revision ?: 0,
                    syncedRevision = existing?.form?.syncedRevision ?: 0), copy.toFieldEntities())
            }
            val remoteIds = forms.map { it.id }.toSet()
            for ((id, row) in local) if (id !in remoteIds && row.form.revision == row.form.syncedRevision) {
                dao.deleteDraft(id)
                row.form.coverPath?.let { paths += it }
            }
            paths
        }
        removed.forEach { cleanup(it) }
    }
    private suspend fun cleanup(path: String) {
        if (dao.coverReferences(path) == 0) {
            try { covers?.delete(path) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { /* Committed data is independent of best-effort file cleanup. */ }
        }
    }
}
