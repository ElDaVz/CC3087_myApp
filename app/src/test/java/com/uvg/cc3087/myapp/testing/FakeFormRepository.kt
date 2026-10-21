package com.uvg.cc3087.myapp.testing

import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.toSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeFormRepository(initialDrafts: List<FormDraft> = emptyList()) : FormRepository {
    private val drafts = MutableStateFlow(initialDrafts.associateBy { it.id })
    val savedDrafts = mutableListOf<FormDraft>()
    var saveFailure: Exception? = null
    var loadFailure: Exception? = null
    var saveDelayMillis = 0L
    var loadDelayMillis = 0L

    override fun observeForms() = drafts.map { stored -> stored.values.map { it.toSummary() } }

    override suspend fun getDraft(id: String): FormDraft? {
        delay(loadDelayMillis)
        loadFailure?.let { throw it }
        return drafts.value[id]
    }

    override suspend fun saveDraft(draft: FormDraft) {
        delay(saveDelayMillis)
        saveFailure?.let { throw it }
        val snapshot = draft.copy(fields = draft.fields.map { it.copy(options = it.options.toList()) })
        savedDrafts += snapshot
        drafts.update { it + (draft.id to snapshot) }
    }

    override suspend fun deleteDraft(id: String) {
        drafts.update { it - id }
    }
}
