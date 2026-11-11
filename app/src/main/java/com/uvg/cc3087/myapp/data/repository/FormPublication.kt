package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormDraft

data class FormPublication(val url: String, val status: String, val version: Long,
    val firstPublishedAt: Long, val matchesDraft: Boolean, val coverUrl: String? = null)
class PublicationException(message: String) : Exception(message)
interface FormPublisher {
    suspend fun publication(owner: String, form: FormDraft): FormPublication?
    suspend fun publish(owner: String, form: FormDraft): FormPublication
    suspend fun archive(owner: String, formId: String)
}
