package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormSyncEngineTest {
    private val form = FormDraft("f", "Pedido", emptyList(), 1, 1, ownerId = "a")
    private class Local(var queue: List<PendingForm>) : LocalFormStore {
        val cover = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        override fun observePublishedCoverUrl(owner: String?, id: String) = cover
        var merged = false
        var acknowledged = false
        override fun observe(owner: String?) = flowOf(emptyList<FormSummary>())
        override fun pendingCount(owner: String) = flowOf(queue.size)
        override suspend fun get(owner: String?, id: String): FormDraft? = null
        override suspend fun save(owner: String?, draft: FormDraft) = Unit
        override suspend fun delete(owner: String?, id: String) = Unit
        override suspend fun importGuests(owner: String, ids: Set<String>) = Unit
        override suspend fun pending(owner: String) = queue.toList()
        override suspend fun acknowledge(owner: String, pending: PendingForm) { acknowledged = true; queue = queue.filterNot { it.revision == pending.revision } }
        override suspend fun mergeRemote(owner: String, forms: List<FormDraft>) { merged = true }
    }
    @Test fun coverObservationStopsOnSessionChange() = runTest {
        val auth = object : AuthRepository {
            override val configured = true
            override val state = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.SignedIn(UserSession("a", "a@example.com")))
            override suspend fun signIn(email: String, password: String) = Unit
            override suspend fun signUp(email: String, password: String) = Unit
            override suspend fun signOut() { state.value = AuthState.Guest }
        }
        val local = Local(emptyList())
        val repository = OfflineFirstFormRepository(local, auth, "a") {}
        val seen = mutableListOf<String?>()
        val job = backgroundScope.launch { repository.observePublishedCoverUrl("f").collect { seen += it } }
        runCurrent()
        local.cover.value = "cover-a"
        runCurrent()
        assertEquals("cover-a", seen.last())
        assertEquals(1, local.cover.subscriptionCount.value)
        auth.state.value = AuthState.SignedIn(UserSession("b", "b@example.com"))
        runCurrent()
        assertNull(seen.last())
        assertEquals(0, local.cover.subscriptionCount.value)
        local.cover.value = "late-cover-a"
        runCurrent()
        assertNull(seen.last())
        job.cancel()
    }

    @Test fun cancellingAWorkerLeavesPendingDataAndClearsTheBusyIndicator() = runTest {
        val local = Local(listOf(PendingForm(form, 1, false)))
        val gate = CompletableDeferred<Unit>()
        val remote = object : RemoteFormSource {
            override suspend fun fetch(owner: String) = listOf(form)
            override suspend fun put(owner: String, form: FormDraft) { gate.await() }
            override suspend fun delete(owner: String, id: String) = Unit
        }
        val engine = FormSyncEngine(local, remote) { "a" }
        val job = launch { engine.sync("a") }
        runCurrent()
        assertEquals(SyncPhase.SYNCING, engine.state.value.phase)
        job.cancel(); runCurrent()
        assertEquals(SyncPhase.IDLE, engine.state.value.phase)
        assertEquals(1, local.queue.size)
        assertFalse(local.acknowledged)
    }

    @Test fun serverFailureLeavesTheDurableQueueAndRetryUsesTheSameDocumentId() = runTest {
        val local = Local(listOf(PendingForm(form, 1, false)))
        var fail = true
        val ids = mutableListOf<String>()
        val remote = object : RemoteFormSource {
            override suspend fun fetch(owner: String) = listOf(form)
            override suspend fun put(owner: String, form: FormDraft) { ids += form.id; if (fail) error("network") }
            override suspend fun delete(owner: String, id: String) = Unit
        }
        val engine = FormSyncEngine(local, remote) { "a" }
        assertEquals(SyncResult.RETRY, engine.sync("a"))
        assertEquals(1, local.queue.size)
        assertFalse(local.merged)
        assertEquals(SyncPhase.ERROR, engine.state.value.phase)
        fail = false
        assertEquals(SyncResult.SUCCESS, engine.sync("a"))
        assertEquals(listOf("f", "f"), ids)
        assertTrue(local.queue.isEmpty()); assertTrue(local.merged)
    }
    @Test fun changedSessionCannotAcknowledgeOrMergeAnOldNetworkResult() = runTest {
        val local = Local(listOf(PendingForm(form, 1, false)))
        var activeOwner = "a"
        val remote = object : RemoteFormSource {
            override suspend fun fetch(owner: String) = error("Must not fetch after a session change")
            override suspend fun put(owner: String, form: FormDraft) = Unit
            override suspend fun delete(owner: String, id: String) = Unit
        }
        val switching = object : RemoteFormSource by remote {
            override suspend fun put(owner: String, form: FormDraft) { activeOwner = "b" }
        }
        val engine = FormSyncEngine(local, switching) { activeOwner }
        assertEquals(SyncResult.SESSION_CHANGED, engine.sync("a"))
        assertFalse(local.acknowledged); assertFalse(local.merged)
        assertEquals(1, local.queue.size)
    }
    @Test fun anEditDuringUploadKeepsItsNewRevisionPending() = runTest {
        val local = Local(listOf(PendingForm(form, 1, false)))
        val remote = object : RemoteFormSource {
            override suspend fun fetch(owner: String) = listOf(form)
            override suspend fun put(owner: String, form: FormDraft) { local.queue = listOf(PendingForm(form.copy(title = "New"), 2, false)) }
            override suspend fun delete(owner: String, id: String) = Unit
        }
        assertEquals(SyncResult.SUCCESS, FormSyncEngine(local, remote) { "a" }.sync("a"))
        assertEquals(2L, local.queue.single().revision)
    }
}
