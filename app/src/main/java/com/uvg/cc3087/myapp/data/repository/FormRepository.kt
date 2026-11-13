package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.data.model.FormDraft
import kotlinx.coroutines.flow.Flow

// las pantallas conocen este contrato, no las tablas ni las consultas de room
interface FormRepository {
    fun observeForms(): Flow<List<FormSummary>>
    suspend fun getDraft(id: String): FormDraft?
    suspend fun saveDraft(draft: FormDraft)
    fun observePublishedCoverUrl(id: String): Flow<String?> = kotlinx.coroutines.flow.flowOf(null)
    suspend fun publication(id: String): FormPublication? = null
    suspend fun publish(id: String): FormPublication = throw PublicationException(PublicationError.SETUP_REQUIRED)
    suspend fun archive(id: String) { throw PublicationException(PublicationError.UNAVAILABLE) }
    /** Removes the form locally and queues public closure for account forms.
     * Received request snapshots and reading history survive. Cloudinary assets are retained.
     */
    suspend fun deleteDraft(id: String)
}
