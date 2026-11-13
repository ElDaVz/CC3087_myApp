package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormAnswer
import com.uvg.cc3087.myapp.data.model.RequestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

/** Requests in the configured data source; the local implementation stays on this device. */
fun interface RequestRepository {
    /** Both IDs identify a response; null means not found. */
    suspend fun getDetail(formId: String, responseId: String): RequestDetail?

    /** Local reading state only; requesting refresh schedules the existing sync mechanism. */
    suspend fun markRead(formId: String, responseId: String) = Unit
    fun requestRefresh() = Unit

    suspend fun getRequests(): List<RequestSummary> = throw RequestBackendUnavailableException()
    fun observeRequests(): Flow<List<RequestSummary>> = flow { emit(getRequests()) }
    suspend fun submit(form: FormDraft, responseId: String, answers: Map<String, FormAnswer>, locale: Locale): RequestDetail =
        throw RequestBackendUnavailableException()
    suspend fun updateStatus(formId: String, responseId: String, status: RequestStatus): RequestDetail =
        throw RequestBackendUnavailableException()
}

class RequestAccessDeniedException : Exception("Response access denied")

class RequestBackendUnavailableException : Exception("Response source is not configured")

object UnavailableRequestRepository : RequestRepository {
    override suspend fun getDetail(formId: String, responseId: String): RequestDetail? =
        throw RequestBackendUnavailableException()
}
