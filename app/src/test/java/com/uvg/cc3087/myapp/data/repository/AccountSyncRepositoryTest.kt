package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSyncRepositoryTest {
    private class Fixture {
        val online = MutableStateFlow(true)
        val changes = MutableStateFlow(AccountPendingChanges())
        var owner: String? = "a"
        var tick = 123L
        var calls = 0
        var stages: suspend (String) -> SyncResult = { SyncResult.SUCCESS }
        val repository = DefaultAccountSyncRepository(online, { changes }, { changes.value }, { owner }, {},
            { calls++; stages(it) }, { tick })
        suspend fun state() = repository.observe("a").first()
    }
    @Test fun uncheckedAfterRestartAndTimestampOnlyAfterCompletePass() = runTest {
        val f = Fixture()
        assertEquals(AccountSyncPhase.UNCHECKED, f.state().phase)
        assertNull(f.state().lastSuccessfulCheck)
        val gate = CompletableDeferred<Unit>()
        f.stages = { gate.await(); SyncResult.SUCCESS }
        val run = async { f.repository.run("a") }
        runCurrent()
        assertEquals(AccountSyncPhase.SYNCING, f.state().phase)
        assertNull(f.state().lastSuccessfulCheck)
        gate.complete(Unit)
        assertEquals(SyncResult.SUCCESS, run.await())
        assertEquals(AccountSyncPhase.UP_TO_DATE, f.state().phase)
        assertEquals(123L, f.state().lastSuccessfulCheck)
        assertNull(Fixture().state().lastSuccessfulCheck)
    }
    @Test fun failuresAtAnyStageNeverReportSuccess() = runTest {
        for (failingStage in 0..2) {
            val f = Fixture()
            var finished = 0
            suspend fun stage(index: Int) { if (index == failingStage) error("backend"); finished++ }
            val pass = AccountSyncPass({ f.owner }, { stage(0); SyncResult.SUCCESS }, { stage(1) }, { stage(2) })
            f.stages = pass::run
            assertEquals(SyncResult.RETRY, f.repository.run("a"))
            assertEquals(failingStage, finished)
            assertEquals(AccountSyncPhase.ERROR, f.state().phase)
            assertNull(f.state().lastSuccessfulCheck)
        }
    }
    @Test fun passWaitsForPublicationsAndResponsesAndStopsAtSessionChange() = runTest {
        val f = Fixture()
        val publications = CompletableDeferred<Unit>()
        val responses = CompletableDeferred<Unit>()
        var responseCalls = 0
        f.stages = AccountSyncPass({ f.owner }, { SyncResult.SUCCESS }, { publications.await() },
            { responseCalls++; responses.await() })::run
        val run = async { f.repository.run("a") }
        runCurrent()
        assertEquals(0, responseCalls)
        assertNull(f.state().lastSuccessfulCheck)
        publications.complete(Unit); runCurrent()
        assertEquals(1, responseCalls)
        assertEquals(AccountSyncPhase.SYNCING, f.state().phase)
        assertNull(f.state().lastSuccessfulCheck)
        responses.complete(Unit)
        assertEquals(SyncResult.SUCCESS, run.await())
        var owner: String? = "a"
        val pass = AccountSyncPass({ owner }, { owner = "b"; SyncResult.SUCCESS },
            { error("Must not refresh another session") }, { error("Must not sync another session") })
        assertEquals(SyncResult.SESSION_CHANGED, pass.run("a"))
    }
    @Test fun disconnectionIsDistinctFromBackendFailureAndCanRecover() = runTest {
        val f = Fixture()
        f.online.value = false
        f.changes.value = AccountPendingChanges(2, 1, 3)
        assertEquals(AccountSyncPhase.WAITING_CONNECTION, f.state().phase)
        assertEquals(6, f.state().pending.total)
        assertEquals(SyncResult.RETRY, f.repository.run("a"))
        assertEquals(0, f.calls)
        f.online.value = true
        f.stages = { error("unavailable") }
        assertEquals(SyncResult.RETRY, f.repository.run("a"))
        assertEquals(AccountSyncPhase.ERROR, f.state().phase)
        f.changes.value = AccountPendingChanges()
        f.stages = { SyncResult.SUCCESS }
        assertEquals(SyncResult.SUCCESS, f.repository.run("a"))
    }
    @Test fun editDuringPassRemainsQueuedAndDoesNotAdvanceLastCheck() = runTest {
        val f = Fixture()
        f.repository.run("a")
        f.tick = 456
        f.stages = { f.changes.value = AccountPendingChanges(responseStatuses = 1); SyncResult.SUCCESS }
        assertEquals(SyncResult.RETRY, f.repository.run("a"))
        assertEquals(AccountSyncPhase.QUEUED, f.state().phase)
        assertEquals(123L, f.state().lastSuccessfulCheck)
        f.changes.value = AccountPendingChanges()
        f.stages = { SyncResult.SUCCESS }
        assertEquals(SyncResult.SUCCESS, f.repository.run("a"))
        assertEquals(456L, f.state().lastSuccessfulCheck)
    }
    @Test fun sameAccountNewSessionDiscardsLateResultsAndLastCheck() = runTest {
        val f = Fixture()
        val gate = CompletableDeferred<Unit>()
        f.stages = { gate.await(); SyncResult.SUCCESS }
        val run = async { f.repository.run("a") }
        runCurrent()
        f.owner = "b"
        f.repository.resetSession()
        assertEquals(AccountSyncPhase.UNCHECKED, f.repository.observe("b").first().phase)
        f.owner = "a"
        f.repository.resetSession()
        gate.complete(Unit)
        assertEquals(SyncResult.SESSION_CHANGED, run.await())
        assertEquals(AccountSyncPhase.UNCHECKED, f.state().phase)
        assertNull(f.state().lastSuccessfulCheck)
    }
    @Test fun cancellationCannotLeaveRunningOrSuccessfulState() = runTest {
        val f = Fixture()
        f.stages = { awaitCancellation() }
        val run = launch { f.repository.run("a") }
        runCurrent()
        run.cancelAndJoin()
        assertEquals(AccountSyncPhase.QUEUED, f.state().phase)
        assertNull(f.state().lastSuccessfulCheck)
    }
}
