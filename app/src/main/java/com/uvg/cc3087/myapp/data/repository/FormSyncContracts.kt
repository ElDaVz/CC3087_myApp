package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.flow.Flow

data class PendingForm(val draft: FormDraft, val revision: Long, val deleted: Boolean)
interface RemoteFormSource {
    suspend fun fetch(owner: String): List<FormDraft>
    suspend fun put(owner: String, form: FormDraft)
    suspend fun delete(owner: String, id: String)
}
interface LocalFormStore {
    fun observe(owner: String?): Flow<List<FormSummary>>
    fun pendingCount(owner: String): Flow<Int>
    suspend fun get(owner: String?, id: String): FormDraft?
    suspend fun save(owner: String?, draft: FormDraft)
    suspend fun delete(owner: String?, id: String)
    suspend fun pending(owner: String): List<PendingForm>
    suspend fun acknowledge(owner: String, pending: PendingForm)
    suspend fun mergeRemote(owner: String, forms: List<FormDraft>)
    suspend fun importGuests(owner: String, ids: Set<String>)
}
