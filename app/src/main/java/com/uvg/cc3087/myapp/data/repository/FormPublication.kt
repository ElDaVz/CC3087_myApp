package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormDraft

data class FormPublication(val url: String, val status: String, val version: Long,
    val firstPublishedAt: Long, val matchesDraft: Boolean, val coverUrl: String? = null)
enum class PublicationError {
    SIGN_IN_REQUIRED,
    FORM_NOT_FOUND,
    BACKEND_NOT_CONFIGURED,
    SETUP_REQUIRED,
    UNAVAILABLE,
    INVALID_FORM,
    TOO_MANY_FIELDS,
    TEXT_TOO_LONG,
    COVER_PREPARATION_FAILED,
    COVER_NOT_CONFIGURED,
    COVER_UPLOAD_FAILED,
    DRAFT_CHANGED,
    PUBLISH_FAILED,
    ARCHIVE_FAILED,
}

class PublicationException(val reason: PublicationError) : Exception(reason.name)
interface FormPublisher {
    suspend fun publication(owner: String, form: FormDraft): FormPublication?
    suspend fun publish(owner: String, form: FormDraft): FormPublication
    suspend fun archive(owner: String, formId: String)
}
