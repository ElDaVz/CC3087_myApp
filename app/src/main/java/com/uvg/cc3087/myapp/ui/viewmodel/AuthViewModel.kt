package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER, RECOVERY }
enum class AuthFieldError { REQUIRED, EMAIL_FORMAT, PASSWORD_LENGTH, PASSWORD_MISMATCH }
data class AuthUiState(
    val email: String = "", val password: String = "", val confirmation: String = "",
    val mode: AuthMode = AuthMode.LOGIN, val busy: Boolean = false,
    val emailError: AuthFieldError? = null, val passwordError: AuthFieldError? = null,
    val confirmationError: AuthFieldError? = null, val error: AuthFailure? = null,
    val unavailable: Boolean = false, val recoverySent: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val uiState = mutableState.asStateFlow()
    private var request: Job? = null
    private var generation = 0L
    private fun emailError(value: String) = when {
        value.trim().isEmpty() -> AuthFieldError.REQUIRED
        !value.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) -> AuthFieldError.EMAIL_FORMAT
        else -> null
    }
    private fun passwordError(value: String, mode: AuthMode) = when {
        value.isEmpty() -> AuthFieldError.REQUIRED
        mode == AuthMode.REGISTER && value.length < 6 -> AuthFieldError.PASSWORD_LENGTH
        else -> null
    }
    private fun confirmationError(value: String, password: String) = when {
        value.isEmpty() -> AuthFieldError.REQUIRED
        value != password -> AuthFieldError.PASSWORD_MISMATCH
        else -> null
    }
    fun email(value: String) { val s = mutableState.value; if (!s.busy) mutableState.value = s.copy(email = value, emailError = emailError(value), error = null) }
    fun password(value: String) {
        val s = mutableState.value
        if (!s.busy) mutableState.value = s.copy(password = value, passwordError = passwordError(value, s.mode),
            confirmationError = if (s.confirmation.isNotEmpty()) confirmationError(s.confirmation, value) else s.confirmationError, error = null)
    }
    fun confirmation(value: String) { val s = mutableState.value; if (!s.busy) mutableState.value = s.copy(confirmation = value, confirmationError = confirmationError(value, s.password), error = null) }
    fun mode(mode: AuthMode) {
        val s = mutableState.value
        if (!s.busy) mutableState.value = AuthUiState(email = s.email, mode = mode)
    }
    fun clearCredentials() {
        generation++
        request?.cancel(); request = null
        mutableState.value = AuthUiState()
    }
    fun submit() {
        val s = mutableState.value
        if (s.busy || s.recoverySent) return
        if (!repository.configured) { mutableState.value = s.copy(unavailable = true); return }
        val checked = s.copy(email = s.email.trim(), emailError = emailError(s.email),
            passwordError = if (s.mode != AuthMode.RECOVERY) passwordError(s.password, s.mode) else null,
            confirmationError = if (s.mode == AuthMode.REGISTER) confirmationError(s.confirmation, s.password) else null,
            error = null)
        mutableState.value = checked
        if (checked.emailError != null || checked.passwordError != null || checked.confirmationError != null) return
        mutableState.value = checked.copy(busy = true)
        val stamp = generation
        request = viewModelScope.launch {
            try {
                when (s.mode) {
                    AuthMode.LOGIN -> repository.signIn(checked.email, s.password)
                    AuthMode.REGISTER -> repository.signUp(checked.email, s.password)
                    AuthMode.RECOVERY -> repository.sendPasswordReset(checked.email)
                }
                if (stamp == generation) {
                    mutableState.value = if (s.mode == AuthMode.RECOVERY) AuthUiState(email = checked.email, mode = s.mode, recoverySent = true) else AuthUiState()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (stamp == generation) mutableState.value = AuthUiState(email = checked.email, mode = s.mode,
                    error = (error as? AuthException)?.reason ?: AuthFailure.UNEXPECTED)
            }
        }
    }
    companion object { fun factory(repository: AuthRepository) = viewModelFactory { initializer { AuthViewModel(repository) } } }
}
