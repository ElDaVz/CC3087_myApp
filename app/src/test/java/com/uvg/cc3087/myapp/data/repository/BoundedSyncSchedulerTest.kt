package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BoundedSyncSchedulerTest {
    @Test fun repeatedRequestsHaveOneSuccessorAndEditsDuringSuccessorQueueAnotherPass() = runTest {
        var unfinished = 0
        var enqueued = 0
        val scheduler = BoundedSyncScheduler({ true }, { unfinished }, { yield(); unfinished++; enqueued++ }, {})
        coroutineScope { repeat(100) { launch { scheduler.request("a") } } }
        assertEquals(2, unfinished)
        assertEquals(2, enqueued)
        // First pass finishes. An edit while the successor runs gets its own durable pass.
        unfinished--
        scheduler.request("a")
        assertEquals(2, unfinished)
        assertEquals(3, enqueued)
    }
    @Test fun accountChangeDuringSchedulingAndCancellationDoNotEnqueueOldWork() = runTest {
        var current: String? = "a"
        var enqueued = 0
        var cancelled: String? = null
        val scheduler = BoundedSyncScheduler({ it == current }, { current = "b"; 0 }, { enqueued++ }, { cancelled = it })
        scheduler.request("a")
        assertEquals(0, enqueued)
        scheduler.cancel("a")
        assertEquals("a", cancelled)
    }
}
