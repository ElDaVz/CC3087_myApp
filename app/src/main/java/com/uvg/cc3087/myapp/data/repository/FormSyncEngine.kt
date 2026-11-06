package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

enum class SyncPhase { IDLE, SYNCING, SYNCED, ERROR }
data class FormSyncState(val owner: String? = null, val phase: SyncPhase = SyncPhase.IDLE)
enum class SyncResult { SUCCESS, RETRY, SESSION_CHANGED }

class FormSyncEngine(private val local: LocalFormStore, private val remote: RemoteFormSource,
    private val timeoutMillis: Long = 30_000, private val currentOwner: () -> String?) {
    private val mutex = Mutex()
    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }
    private val mutableState = MutableStateFlow(FormSyncState())
    val state = mutableState.asStateFlow()
    fun schedulingFailed(owner: String) { mutableState.value = FormSyncState(owner, SyncPhase.ERROR) }
    private class SessionChanged : Exception()
    private fun checkSession(owner: String) { if (currentOwner() != owner) throw SessionChanged() }

    suspend fun sync(owner: String): SyncResult = mutex.withLock {
        try {
            checkSession(owner)
            mutableState.value = FormSyncState(owner, SyncPhase.SYNCING)
            withTimeout(timeoutMillis) {
                for (pending in local.pending(owner)) {
                    checkSession(owner)
                    if (pending.deleted) remote.delete(owner, pending.draft.id)
                    else remote.put(owner, pending.draft)
                    checkSession(owner)
                    // The store acknowledges only this revision, never a newer pending edit.
                    local.acknowledge(owner, pending)
                }
                checkSession(owner)
                val fetched = remote.fetch(owner)
                checkSession(owner)
                local.mergeRemote(owner, fetched)
                checkSession(owner)
                mutableState.value = FormSyncState(owner, SyncPhase.SYNCED)
            }
            SyncResult.SUCCESS
        } catch (_: SessionChanged) { SyncResult.SESSION_CHANGED }
        catch (_: TimeoutCancellationException) {
            if (currentOwner() == owner) mutableState.value = FormSyncState(owner, SyncPhase.ERROR)
            SyncResult.RETRY
        } catch (cancelled: CancellationException) {
            if (currentOwner() == owner) mutableState.value = FormSyncState(owner, SyncPhase.IDLE)
            throw cancelled
        }
        catch (_: Exception) {
            if (currentOwner() == owner) mutableState.value = FormSyncState(owner, SyncPhase.ERROR)
            SyncResult.RETRY
        }
    }
}
