package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.data.model.FormDraft
import kotlinx.coroutines.flow.Flow

// las pantallas conocen este contrato, no las tablas ni las consultas de room
interface FormRepository {
    fun observeForms(): Flow<List<FormSummary>>
    suspend fun getDraft(id: String): FormDraft?
    suspend fun saveDraft(draft: FormDraft)
    suspend fun publication(id: String): FormPublication? = null
    suspend fun publish(id: String): FormPublication = throw PublicationException("Inicia sesión y configura Firebase para publicar")
    suspend fun archive(id: String) { throw PublicationException("La publicación no está disponible") }
    suspend fun deleteDraft(id: String)
}
