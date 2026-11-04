package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.AuthRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun clear() { store.clear() }
    private class FakeAuth(override val configured: Boolean = true) : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.Guest)
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        var fail = false
        override suspend fun signIn(email: String, password: String) {
            calls++; gate.await()
            if (fail) error("network")
            state.value = AuthState.SignedIn(UserSession("uid", email))
        }
        override suspend fun signUp(email: String, password: String) = signIn(email, password)
        override suspend fun signOut() { state.value = AuthState.Guest }
    }
    @Test fun validatesAndPreventsDoubleSubmissionWithoutPersistingPasswords() = runTest {
        val repo = FakeAuth()
        val model = AuthViewModel(repo).also { store.put("auth", it) }
        model.submit()
        assertEquals(AuthError.INVALID_INPUT, model.uiState.value.error)
        assertEquals(0, repo.calls)
        model.email("ana@example.com"); model.password("secret123")
        model.submit(); model.submit(); runCurrent()
        assertEquals(1, repo.calls)
        assertTrue(model.uiState.value.busy)
        repo.gate.complete(Unit); runCurrent()
        assertEquals("", model.uiState.value.password)
        assertFalse(model.uiState.value.busy)
        assertTrue(repo.state.value is AuthState.SignedIn)
    }
    @Test fun configurationAndNetworkFailuresAreVisibleAndRetryable() = runTest {
        val unavailable = AuthViewModel(FakeAuth(false)).also { store.put("unavailable", it) }
        unavailable.submit()
        assertEquals(AuthError.NOT_CONFIGURED, unavailable.uiState.value.error)
        val repo = FakeAuth().apply { fail = true; gate.complete(Unit) }
        val model = AuthViewModel(repo).also { store.put("auth", it) }
        model.email("ana@example.com"); model.password("secret123"); model.submit(); runCurrent()
        assertEquals(AuthError.FAILED, model.uiState.value.error)
        assertFalse(model.uiState.value.busy)
        assertEquals("", model.uiState.value.password)
        repo.fail = false; model.password("secret123"); model.submit(); runCurrent()
        assertTrue(repo.state.value is AuthState.SignedIn)
    }
}
