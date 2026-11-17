package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class AccountSyncPhase { UNCHECKED, QUEUED, WAITING_CONNECTION, SYNCING, UP_TO_DATE, ERROR }
data class AccountPendingChanges(val drafts: Int = 0, val deletions: Int = 0, val responseStatuses: Int = 0) {
    val total get() = drafts + deletions + responseStatuses
}
data class AccountSyncState(
    val phase: AccountSyncPhase = AccountSyncPhase.UNCHECKED,
    val pending: AccountPendingChanges = AccountPendingChanges(),
    val lastSuccessfulCheck: Long? = null
)
interface AccountSyncRepository {
    fun observe(owner: String): Flow<AccountSyncState>
    fun requestSync(owner: String)
}

/** Each remote stage must finish before the account can be checked for remaining local changes. */
class AccountSyncPass(
    private val currentOwner: () -> String?,
    private val forms: suspend (String) -> SyncResult,
    private val publications: suspend (String) -> Unit,
    private val responses: suspend (String) -> Unit
) {
    suspend fun run(owner: String): SyncResult {
        if (currentOwner() != owner) return SyncResult.SESSION_CHANGED
        val result = forms(owner)
        if (result != SyncResult.SUCCESS) return result
        if (currentOwner() != owner) return SyncResult.SESSION_CHANGED
        publications(owner)
        if (currentOwner() != owner) return SyncResult.SESSION_CHANGED
        responses(owner)
        return if (currentOwner() == owner) SyncResult.SUCCESS else SyncResult.SESSION_CHANGED
    }
}

/** Success covers the entire account, never just the form upload stage. Session-only timestamps. */
class DefaultAccountSyncRepository(
    private val connected: StateFlow<Boolean>,
    private val pending: (String) -> Flow<AccountPendingChanges>,
    private val snapshot: suspend (String) -> AccountPendingChanges,
    private val currentOwner: () -> String?,
    private val schedule: (String) -> Unit,
    private val synchronize: suspend (String) -> SyncResult,
    private val now: () -> Long = System::currentTimeMillis
) : AccountSyncRepository {
    private data class Activity(val owner: String? = null, val phase: AccountSyncPhase = AccountSyncPhase.UNCHECKED,
        val checked: Long? = null, val session: Long = 0)
    private val activity = MutableStateFlow(Activity())
    private val mutex = Mutex()
    // Changing session invalidates both in-flight results and the previous successful check.
    private val generation = java.util.concurrent.atomic.AtomicLong()
    fun resetSession() { activity.value = Activity(session = generation.incrementAndGet()) }
    override fun observe(owner: String): Flow<AccountSyncState> = combine(pending(owner), connected, activity) { count, online, active ->
        val own = active.takeIf { it.owner == owner && it.session == generation.get() } ?: Activity()
        val phase = when {
            !online -> AccountSyncPhase.WAITING_CONNECTION
            own.phase == AccountSyncPhase.SYNCING -> AccountSyncPhase.SYNCING
            own.phase == AccountSyncPhase.ERROR -> AccountSyncPhase.ERROR
            count.total > 0 -> AccountSyncPhase.QUEUED
            else -> own.phase
        }
        AccountSyncState(phase, count, own.checked)
    }.distinctUntilChanged()
    override fun requestSync(owner: String) {
        if (currentOwner() != owner) return
        val old = activity.value.takeIf { it.owner == owner && it.session == generation.get() } ?: Activity(owner, session = generation.get())
        if (old.phase != AccountSyncPhase.SYNCING) activity.value = old.copy(phase = AccountSyncPhase.QUEUED)
        schedule(owner)
    }
    fun schedulingFailed(owner: String) {
        if (currentOwner() == owner) activity.value = Activity(owner, AccountSyncPhase.ERROR,
            activity.value.takeIf { it.owner == owner && it.session == generation.get() }?.checked, generation.get())
    }
    suspend fun run(owner: String): SyncResult = mutex.withLock {
        val stamp = generation.get()
        fun sameSession() = currentOwner() == owner && stamp == generation.get()
        if (!sameSession()) return@withLock SyncResult.SESSION_CHANGED
        if (!connected.value) return@withLock SyncResult.RETRY
        val last = activity.value.takeIf { it.owner == owner && it.session == generation.get() }?.checked
        activity.value = Activity(owner, AccountSyncPhase.SYNCING, last, stamp)
        try {
            val result = synchronize(owner)
            if (!sameSession()) return@withLock SyncResult.SESSION_CHANGED
            if (result != SyncResult.SUCCESS) {
                activity.value = Activity(owner, AccountSyncPhase.ERROR, last, stamp)
                return@withLock result
            }
            val remaining = snapshot(owner)
            if (!sameSession()) return@withLock SyncResult.SESSION_CHANGED
            activity.value = if (remaining.total == 0) Activity(owner, AccountSyncPhase.UP_TO_DATE, now(), stamp)
                else Activity(owner, AccountSyncPhase.QUEUED, last, stamp)
            if (remaining.total == 0) SyncResult.SUCCESS else SyncResult.RETRY
        } catch (cancelled: CancellationException) {
            if (sameSession()) activity.value = Activity(owner, AccountSyncPhase.QUEUED, last, stamp)
            throw cancelled
        } catch (_: Exception) {
            if (sameSession()) activity.value = Activity(owner, AccountSyncPhase.ERROR, last, stamp)
            if (sameSession()) SyncResult.RETRY else SyncResult.SESSION_CHANGED
        }
    }
}
