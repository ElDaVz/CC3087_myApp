package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.data.model.FormDraft
import kotlinx.coroutines.flow.Flow

// las pantallas conocen este contrato, no las tablas ni las consultas de room
interface FormRepository {
    fun observeForms(): Flow<List<FormSummary>>
    suspend fun getDraft(id: String): FormDraft?
    suspend fun saveDraft(draft: FormDraft)
    suspend fun deleteDraft(id: String)
}
