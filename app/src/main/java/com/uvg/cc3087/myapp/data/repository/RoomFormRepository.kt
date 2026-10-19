package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.local.FormDraftDao
import com.uvg.cc3087.myapp.data.local.toDraft
import com.uvg.cc3087.myapp.data.local.toEntity
import com.uvg.cc3087.myapp.data.local.toFieldEntities
import com.uvg.cc3087.myapp.data.model.FormDraft
import kotlinx.coroutines.flow.map

class RoomFormRepository(private val dao: FormDraftDao) : FormRepository {
    override fun observeForms() = dao.observeDrafts().map { drafts ->
        drafts.map { it.toDraft().toSummary() }
    }

    override suspend fun getDraft(id: String): FormDraft? = dao.getDraft(id)?.toDraft()

    override suspend fun saveDraft(draft: FormDraft) {
        require(draft.id.isNotBlank()) { "A draft needs an id" }
        require(draft.fields.map { it.id }.distinct().size == draft.fields.size) {
            "Field ids must be unique within a draft"
        }
        dao.saveDraft(draft.toEntity(), draft.toFieldEntities())
    }

    override suspend fun deleteDraft(id: String) = dao.deleteDraft(id)
}
