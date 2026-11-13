package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuestDraftImporterTest {
    private class Store : GuestImportStore {
        val counts = MutableStateFlow(GuestImportCounts(pending = 2))
        var calls = 0
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun snapshotGuestIds() = setOf("one", "two")
        override suspend fun prepare(owner: String, ids: Set<String>) = Unit
        override fun observe(owner: String) = if (owner == "a") counts else flowOf(GuestImportCounts())
        override suspend fun transfer(owner: String): Int {
            calls++
            gate?.await()
            check(!fail)
            val moved = counts.value.pending
            counts.value = GuestImportCounts(completed = counts.value.completed + moved)
            return moved
        }
    }

    @Test fun successSchedulesOnceAndDuplicateRequestsDoNotTransferAgain() = runTest {
        val store = Store().apply { gate = CompletableDeferred() }
        val scheduled = mutableListOf<String>()
        val importer = GuestDraftImporter(store, { "a" }, scheduled::add)
        val seen = mutableListOf<GuestImportState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { importer.observe("a").toList(seen) }
        val first = async { importer.resume("a") }
        val second = async { importer.resume("a") }
        runCurrent()
        assertEquals(GuestImportPhase.IMPORTING, seen.last().phase)
        assertEquals(1, store.calls)
        store.gate!!.complete(Unit)
        assertTrue(first.await()); assertTrue(second.await())
        runCurrent()
        assertEquals(GuestImportState(GuestImportPhase.COMPLETE, 2), seen.last())
        assertEquals(listOf("a"), scheduled)
        assertEquals(1, store.calls)
    }

    @Test fun failedImportRetainsPendingAndAnotherInstanceCanResumeIt() = runTest {
        val store = Store().apply { fail = true }
        val importer = GuestDraftImporter(store, { "a" }) {}
        assertFalse(importer.resume("a"))
        assertEquals(GuestImportState(GuestImportPhase.ERROR, 2), importer.observe("a").first())
        assertEquals(2, store.counts.value.pending)
        store.fail = false
        val restored = GuestDraftImporter(store, { "a" }) {}
        assertTrue(restored.resume("a"))
        assertEquals(GuestImportState(GuestImportPhase.COMPLETE, 2), restored.observe("a").first())
    }

    @Test fun anotherAccountCannotResumeOrSeeAnOldAccountsFailure() = runTest {
        val store = Store().apply { fail = true }
        var owner: String? = "a"
        val importer = GuestDraftImporter(store, { owner }) {}
        assertFalse(importer.resume("a"))
        owner = "b"
        assertTrue(importer.resume("a"))
        assertTrue(importer.resume("b"))
        assertEquals(1, store.calls)
        assertEquals(GuestImportState(), importer.observe("b").first())
    }

    @Test fun cancellationLeavesPendingAndClearsProgressForRetry() = runTest {
        val store = Store().apply { gate = CompletableDeferred() }
        val importer = GuestDraftImporter(store, { "a" }) {}
        val job = launch { importer.resume("a") }
        runCurrent(); job.cancelAndJoin()
        assertEquals(GuestImportState(GuestImportPhase.PENDING, 2), importer.observe("a").first())
        store.gate = null
        assertTrue(importer.resume("a"))
        assertEquals(GuestImportPhase.COMPLETE, importer.observe("a").first().phase)
    }

    @Test fun accountChangeDuringTransferDoesNotScheduleUnderOldAccount() = runTest {
        val store = Store().apply { gate = CompletableDeferred() }
        var owner = "a"
        var scheduled = false
        val importer = GuestDraftImporter(store, { owner }) { scheduled = true }
        val job = async { importer.resume("a") }
        runCurrent(); owner = "b"; store.gate!!.complete(Unit)
        assertTrue(job.await())
        assertFalse(scheduled)
        assertEquals(GuestImportState(), importer.observe("b").first())
    }

    @Test fun noPendingWorkAndSchedulerFailureDoNotReportImportFailure() = runTest {
        val store = Store()
        val importer = GuestDraftImporter(store, { "a" }) { error("scheduler unavailable") }
        assertTrue(importer.resume("a"))
        assertTrue(importer.resume("a"))
        assertEquals(1, store.calls)
        assertEquals(GuestImportPhase.COMPLETE, importer.observe("a").first().phase)
    }
}
