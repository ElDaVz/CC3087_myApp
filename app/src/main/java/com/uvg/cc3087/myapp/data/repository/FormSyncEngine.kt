package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

enum class SyncResult { SUCCESS, RETRY, SESSION_CHANGED }

class FormSyncEngine(private val local: LocalFormStore, private val remote: RemoteFormSource,
    private val timeoutMillis: Long = 30_000, private val currentOwner: () -> String?) {
    private val mutex = Mutex()
    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }
    private class SessionChanged : Exception()
    private fun checkSession(owner: String) { if (currentOwner() != owner) throw SessionChanged() }

    suspend fun sync(owner: String): SyncResult = mutex.withLock {
        try {
            checkSession(owner)
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
            }
            SyncResult.SUCCESS
        } catch (_: SessionChanged) { SyncResult.SESSION_CHANGED }
        catch (_: TimeoutCancellationException) {
            SyncResult.RETRY
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
        catch (_: Exception) {
            SyncResult.RETRY
        }
    }
}
