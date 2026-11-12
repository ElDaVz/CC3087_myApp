package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import kotlinx.coroutines.*
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
        var failure: AuthFailure? = null
        var received: Pair<String, String>? = null
        var ignoreCancellation = false
        private suspend fun waitForResult() {
            calls++
            if (ignoreCancellation) withContext(NonCancellable) { gate.await() } else gate.await()
            failure?.let { throw AuthException(it) }
        }
        override suspend fun signIn(email: String, password: String) {
            received = email to password; waitForResult()
            state.value = AuthState.SignedIn(UserSession("uid", email))
        }
        override suspend fun signUp(email: String, password: String) = signIn(email, password)
        override suspend fun sendPasswordReset(email: String) { received = email to ""; waitForResult() }
        override suspend fun signOut() { state.value = AuthState.Guest }
    }
    private fun model(repo: FakeAuth = FakeAuth()) = AuthViewModel(repo).also { store.put("auth", it) }
    @Test fun validatesFieldsAndRegistrationConfirmation() = runTest {
        val repo = FakeAuth(); val model = model(repo)
        model.submit()
        assertEquals(AuthFieldError.REQUIRED, model.uiState.value.emailError)
        assertEquals(AuthFieldError.REQUIRED, model.uiState.value.passwordError)
        model.email("bad@address")
        assertEquals(AuthFieldError.EMAIL_FORMAT, model.uiState.value.emailError)
        model.email("ana@example.com"); model.mode(AuthMode.REGISTER); model.password("12345"); model.confirmation("other")
        model.submit()
        assertEquals(AuthFieldError.PASSWORD_LENGTH, model.uiState.value.passwordError)
        assertEquals(AuthFieldError.PASSWORD_MISMATCH, model.uiState.value.confirmationError)
        model.password("123456"); model.confirmation("123456")
        model.submit(); runCurrent(); assertEquals(1, repo.calls)
        repo.gate.complete(Unit); runCurrent()
        assertEquals(AuthUiState(), model.uiState.value)
    }
    @Test fun trimsOnlyEmailAndBlocksDuplicateSubmissionsAndModeChanges() = runTest {
        val repo = FakeAuth(); val model = model(repo)
        model.email("  ana@example.com  "); model.password(" secret123 ")
        model.submit(); model.submit(); model.mode(AuthMode.RECOVERY); model.email("other@example.com"); runCurrent()
        assertEquals(1, repo.calls)
        assertEquals("ana@example.com" to " secret123 ", repo.received)
        assertEquals(AuthMode.LOGIN, model.uiState.value.mode)
        assertTrue(model.uiState.value.busy)
        repo.gate.complete(Unit); runCurrent()
        assertEquals(AuthUiState(), model.uiState.value)
    }
    @Test fun modeChangesKeepEmailAndClearPasswordsAndErrors() = runTest {
        val model = model()
        model.mode(AuthMode.REGISTER); model.email("ana@example.com"); model.password("short"); model.confirmation("other"); model.submit()
        model.mode(AuthMode.RECOVERY)
        assertEquals(AuthUiState(email = "ana@example.com", mode = AuthMode.RECOVERY), model.uiState.value)
        model.mode(AuthMode.LOGIN)
        assertEquals(AuthUiState(email = "ana@example.com"), model.uiState.value)
        model.clearCredentials(); assertEquals(AuthUiState(), model.uiState.value)
    }
    @Test fun recoveryNeedsOnlyEmailAndNeverSignsInOrSubmitsTwiceAfterSuccess() = runTest {
        val repo = FakeAuth(); val model = model(repo)
        model.email("ana@example.com"); model.mode(AuthMode.RECOVERY); model.submit(); runCurrent()
        repo.gate.complete(Unit); runCurrent(); model.submit(); runCurrent()
        assertTrue(model.uiState.value.recoverySent)
        assertEquals(AuthState.Guest, repo.state.value)
        assertEquals(1, repo.calls)
        model.mode(AuthMode.LOGIN); assertFalse(model.uiState.value.recoverySent)
    }
    @Test fun configurationAndAllFailuresAreVisibleAndCredentialsAreCleared() = runTest {
        val unavailable = model(FakeAuth(false)); unavailable.submit()
        assertTrue(unavailable.uiState.value.unavailable)
        for (failure in AuthFailure.entries) {
            val repo = FakeAuth().apply { this.failure = failure; gate.complete(Unit) }; val model = model(repo)
            model.mode(AuthMode.REGISTER); model.email("ana@example.com"); model.password("secret123"); model.confirmation("secret123")
            model.submit(); runCurrent()
            assertEquals(failure, model.uiState.value.error)
            assertFalse(model.uiState.value.busy)
            assertEquals("", model.uiState.value.password); assertEquals("", model.uiState.value.confirmation)
            assertEquals("ana@example.com", model.uiState.value.email)
            repo.failure = null; model.password("secret123"); model.confirmation("secret123"); model.submit(); runCurrent()
            assertTrue(repo.state.value is AuthState.SignedIn)
        }
    }
    @Test fun closingFlowDiscardsLateRecoverySuccessAndFailure() = runTest {
        for (failure in listOf(null, AuthFailure.NETWORK)) {
            val repo = FakeAuth().apply { ignoreCancellation = true; this.failure = failure }; val model = model(repo)
            model.email("ana@example.com"); model.mode(AuthMode.RECOVERY); model.submit(); runCurrent()
            model.clearCredentials(); model.email("new@example.com")
            repo.gate.complete(Unit); runCurrent()
            assertEquals(AuthUiState(email = "new@example.com"), model.uiState.value)
        }
    }
}
