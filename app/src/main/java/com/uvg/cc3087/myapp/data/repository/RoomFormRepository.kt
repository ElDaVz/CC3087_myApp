package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.local.FormDraftDao
import com.uvg.cc3087.myapp.data.local.toDraft
import com.uvg.cc3087.myapp.data.local.toEntity
import com.uvg.cc3087.myapp.data.local.toFieldEntities
import com.uvg.cc3087.myapp.data.model.FormDraft
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import com.uvg.cc3087.myapp.data.local.RequestDao

class RoomFormRepository(private val dao: FormDraftDao, private val requests: RequestDao? = null, private val covers: CoverStorage? = null) : FormRepository {
    override fun observeForms() = combine(dao.observeDrafts(), requests?.observeCounts() ?: flowOf(emptyList())) { drafts, counts ->
        val byForm = counts.associate { it.formId to it.count }
        drafts.map { it.toDraft().toSummary().copy(responseCount = byForm[it.form.id] ?: 0) }
    }

    override suspend fun getDraft(id: String): FormDraft? = dao.getDraft(id)?.toDraft()

    override suspend fun saveDraft(draft: FormDraft) {
        require(draft.id.isNotBlank()) { "A draft needs an id" }
        require(draft.fields.map { it.id }.distinct().size == draft.fields.size) {
            "Field ids must be unique within a draft"
        }
        val oldCover = dao.getDraft(draft.id)?.form?.coverPath
        dao.saveDraft(draft.toEntity(), draft.toFieldEntities())
        if (oldCover != null && oldCover != draft.coverPath) cleanupCover(oldCover)
    }

    override suspend fun deleteDraft(id: String) {
        val oldCover = dao.getDraft(id)?.form?.coverPath
        dao.deleteDraft(id)
        oldCover?.let { cleanupCover(it) }
    }

    private suspend fun cleanupCover(path: String) {
        if (dao.coverReferences(path) == 0) {
            try { covers?.delete(path) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { /* File cleanup must not turn a committed save into a failure. */ }
        }
    }
}
