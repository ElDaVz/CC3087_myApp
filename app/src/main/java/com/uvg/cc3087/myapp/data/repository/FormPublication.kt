package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormDraft

data class FormPublication(val url: String, val status: String, val version: Long,
    val firstPublishedAt: Long, val matchesDraft: Boolean, val coverUrl: String? = null,
    val links: FormLinks = FormLinks(url), val invitationMode: InvitationMode = InvitationMode.OPEN)
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

class SecretLinkRepository(private val store: LinkStore) {
    suspend fun forForm(owner: String, formId: String, publicUrl: String): FormLinks {
        val existing = store.get(owner, formId)
        if (existing != null) return existing.withBase(publicUrl.substringBeforeLast("/f/"))
        val created = FormLinks(publicUrl, tokenUrl(publicUrl.substringBefore("/f/"), LinkTokens.newToken(), "i/$formId"))
        store.save(owner, formId, created)
        return created
    }

    suspend fun restoreInvitation(owner: String, formId: String, publicUrl: String, token: String) {
        require(LinkTokens.isValid(token))
        val current = forForm(owner, formId, publicUrl)
        store.save(owner, formId, current.copy(singleUseUrl = tokenUrl(publicUrl.substringBefore("/f/"), token, "i/$formId")))
    }

    suspend fun renewInvitation(owner: String, formId: String, publicUrl: String): FormLinks {
        val current = forForm(owner, formId, publicUrl)
        val renewed = current.copy(singleUseUrl = tokenUrl(publicUrl.substringBefore("/f/"), LinkTokens.newToken(), "i/$formId"))
        return renewed
    }
}

interface LinkStore {
    suspend fun get(owner: String, formId: String): FormLinks?
    suspend fun save(owner: String, formId: String, links: FormLinks)
}

interface FormPublisher {
    suspend fun publication(owner: String, form: FormDraft): FormPublication?
    suspend fun publish(owner: String, form: FormDraft): FormPublication
    suspend fun publish(owner: String, form: FormDraft, mode: InvitationMode): FormPublication = publish(owner, form)
    suspend fun archive(owner: String, formId: String)
}
