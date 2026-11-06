package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.*
import kotlinx.coroutines.CancellationException

/** A repository instance is permanently bound to one session partition. */
class OfflineFirstFormRepository(
    private val local: LocalFormStore,
    private val auth: AuthRepository,
    val owner: String?,
    private val schedule: (String) -> Unit
) : FormRepository {
    private fun checkSession() {
        if (auth.state.value is AuthState.Initializing || auth.state.value.userId != owner)
            throw CancellationException("The session changed")
    }
    override fun observeForms() = local.observe(owner)
    override suspend fun getDraft(id: String): FormDraft? { checkSession(); return local.get(owner, id) }
    override suspend fun saveDraft(draft: FormDraft) {
        checkSession()
        local.save(owner, draft)
        owner?.let { schedule(it) }
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
