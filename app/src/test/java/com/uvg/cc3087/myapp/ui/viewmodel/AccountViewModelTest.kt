package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun clear() { store.clear() }
    private val events = mutableListOf<String>()
    private val state = MutableStateFlow(AccountSyncState())
    private val importing = MutableStateFlow(GuestImportState())
    private val auth = object : AuthRepository {
        override val configured = true
        override val state = MutableStateFlow<AuthState>(AuthState.SignedIn(UserSession("a", "a@example.com")))
        override suspend fun signIn(email: String, password: String) = Unit
        override suspend fun signUp(email: String, password: String) = Unit
        override suspend fun sendPasswordReset(email: String) = Unit
        override suspend fun signOut() { events += "out"; state.value = AuthState.Guest }
    }
    private fun model(): AccountViewModel {
        val sync = object : AccountSyncRepository {
            override fun observe(owner: String) = state
            override fun requestSync(owner: String) { events += "sync:$owner" }
        }
        val imports = object : GuestImportRepository {
            override fun observe(owner: String) = importing
            override suspend fun resume(owner: String): Boolean { events += "import:$owner"; return true }
        }
        return AccountViewModel(auth, "a", sync, imports, { events += "cancel:$it" }).also { store.put("account", it) }
    }
    @Test fun pendingChangesRequireConfirmationAndCancelLeavesAccountActive() = runTest {
        state.value = AccountSyncState(pending = AccountPendingChanges(deletions = 1))
        val model = model(); runCurrent()
        model.signOut()
        assertTrue(model.uiState.value.confirmSignOut)
        assertTrue(events.isEmpty())
        model.dismissSignOut()
        assertFalse(model.uiState.value.confirmSignOut)
        assertTrue(auth.state.value is AuthState.SignedIn)
        model.signOut(); model.confirmSignOut(); model.confirmSignOut(); runCurrent()
        assertEquals(listOf("cancel:a", "out"), events)
    }
    @Test fun pendingLocalImportAlsoRequiresConfirmationWithoutRemoteChanges() = runTest {
        importing.value = GuestImportState(GuestImportPhase.ERROR, 2)
        val model = model(); runCurrent()
        model.signOut()
        assertTrue(model.uiState.value.confirmSignOut)
        model.dismissSignOut(); model.retryImport(); model.requestSync(); runCurrent()
        assertEquals(listOf("sync:a", "import:a"), events)
    }
    @Test fun noPendingWorkSignsOutAndCancelsOnlyItsOwner() = runTest {
        val model = model(); runCurrent()
        model.signOut(); runCurrent()
        assertEquals(listOf("cancel:a", "out"), events)
    }
}
