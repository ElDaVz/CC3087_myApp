package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GuestImportCounts(val pending: Int = 0, val completed: Int = 0)
enum class GuestImportPhase { NONE, PENDING, IMPORTING, COMPLETE, ERROR }
data class GuestImportState(val phase: GuestImportPhase = GuestImportPhase.NONE, val count: Int = 0)

/** Durable reservations belong to the confirmed account, including across sign-out. */
interface GuestImportStore {
    suspend fun snapshotGuestIds(): Set<String>
    suspend fun prepare(owner: String, ids: Set<String>)
    fun observe(owner: String): Flow<GuestImportCounts>
    suspend fun transfer(owner: String): Int
}

interface GuestImportRepository {
    fun observe(owner: String): Flow<GuestImportState>
    suspend fun resume(owner: String): Boolean
}

/** Local incorporation needs no network. Synchronization is scheduled only after commit. */
class GuestDraftImporter(private val store: GuestImportStore, private val currentOwner: () -> String?,
    private val schedule: (String) -> Unit) : GuestImportRepository {
    private val mutex = Mutex()
    private val activity = MutableStateFlow<Pair<String, GuestImportPhase>?>(null)

    override fun observe(owner: String): Flow<GuestImportState> = combine(store.observe(owner), activity) { counts, active ->
        val phase = when {
            active?.first == owner && active.second == GuestImportPhase.IMPORTING -> GuestImportPhase.IMPORTING
            counts.pending > 0 && active?.first == owner && active.second == GuestImportPhase.ERROR -> GuestImportPhase.ERROR
            counts.pending > 0 -> GuestImportPhase.PENDING
            counts.completed > 0 -> GuestImportPhase.COMPLETE
            else -> GuestImportPhase.NONE
        }
        GuestImportState(phase, counts.pending + counts.completed)
    }.distinctUntilChanged()

    override suspend fun resume(owner: String): Boolean = mutex.withLock {
        if (currentOwner() != owner) return@withLock true
        try {
            val counts = store.observe(owner).first()
            if (counts.pending == 0 || currentOwner() != owner) return@withLock true
            activity.value = owner to GuestImportPhase.IMPORTING
            store.transfer(owner)
            if (currentOwner() == owner) {
                // A scheduler failure must not turn a committed local import into an error.
                try { schedule(owner) } catch (_: RuntimeException) { /* Foreground/worker retry schedules sync. */ }
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (currentOwner() == owner) activity.value = owner to GuestImportPhase.ERROR
            false
        } finally {
            if (activity.value == (owner to GuestImportPhase.IMPORTING)) activity.value = null
        }
    }
}
