package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Durable WorkManager chain: at most one active pass and one successor, even during an upload. */
class BoundedSyncScheduler(
    private val isCurrent: (String) -> Boolean,
    private val unfinished: suspend (String) -> Int,
    private val enqueue: suspend (String) -> Unit,
    private val cancelWork: suspend (String) -> Unit
) {
    private val mutex = Mutex()
    suspend fun request(owner: String) = mutex.withLock {
        if (isCurrent(owner) && unfinished(owner) < 2 && isCurrent(owner)) enqueue(owner)
    }
    suspend fun cancel(owner: String) = mutex.withLock { cancelWork(owner) }
}
