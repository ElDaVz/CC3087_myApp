package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flatMapLatest

/** A repository instance is permanently bound to one session partition. */
class OfflineFirstFormRepository(
    private val local: LocalFormStore,
    private val auth: AuthRepository,
    val owner: String?,
    private val publisher: FormPublisher? = null,
    private val schedule: (String) -> Unit
) : FormRepository {
    private fun checkSession() {
        if (auth.state.value is AuthState.Initializing || auth.state.value.userId != owner)
            throw CancellationException("The session changed")
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observePublishedCoverUrl(id: String) = auth.state.flatMapLatest { session ->
        if (session !is AuthState.Initializing && session.userId == owner)
            local.observePublishedCoverUrl(owner, id)
        else kotlinx.coroutines.flow.flowOf(null)
    }
    override fun observeForms() = local.observe(owner)
    override suspend fun getDraft(id: String): FormDraft? { checkSession(); return local.get(owner, id) }
    override suspend fun saveDraft(draft: FormDraft) {
        checkSession()
        local.save(owner, draft)
        owner?.let { schedule(it) }
    }
    override suspend fun publication(id: String): FormPublication? {
        checkSession()
        val user = owner ?: return null
        return getDraft(id)?.let { publisher?.publication(user, it) }
    }
    override suspend fun publish(id: String): FormPublication {
        checkSession()
        val user = owner ?: throw PublicationException(PublicationError.SIGN_IN_REQUIRED)
        val form = getDraft(id) ?: throw PublicationException(PublicationError.FORM_NOT_FOUND)
        return (publisher ?: throw PublicationException(PublicationError.BACKEND_NOT_CONFIGURED)).publish(user, form)
    }
    override suspend fun archive(id: String) {
        checkSession()
        checkNotNull(publisher).archive(checkNotNull(owner), id)
    }
    override suspend fun deleteDraft(id: String) {
        checkSession()
        local.delete(owner, id)
        owner?.let { schedule(it) }
    }
    suspend fun importGuests(ids: Set<String>) {
        checkSession()
        val user = checkNotNull(owner)
        local.importGuests(user, ids)
        schedule(user)
    }
}
