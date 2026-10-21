package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

// seguimos usando los mismos ejemplos mientras se integra el guardado local
class SampleFormRepository : FormRepository {
    private val forms = MutableStateFlow(FormSampleData.forms)
    private val drafts = MutableStateFlow<Map<String, FormDraft>>(emptyMap())

    override fun observeForms(): Flow<List<FormSummary>> = combine(forms, drafts) { samples, saved ->
        saved.values.sortedByDescending { it.updatedAt }.map { it.toSummary() } +
            samples.filterNot { it.id in saved }
    }

    override suspend fun getDraft(id: String): FormDraft? = drafts.value[id]?.snapshot()

    override suspend fun saveDraft(draft: FormDraft) {
        drafts.update { it + (draft.id to draft.snapshot()) }
    }

    override suspend fun deleteDraft(id: String) {
        drafts.update { it - id }
    }

    fun updateSampleForms(localizedForms: List<FormSummary>) {
        forms.value = localizedForms.toList()
    }

    private fun FormDraft.snapshot() = copy(fields = fields.map {
        it.copy(options = it.options.toList())
    })
}
