package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthError { INVALID_INPUT, FAILED, NOT_CONFIGURED }
data class AuthUiState(val email: String = "", val password: String = "", val register: Boolean = false,
    val busy: Boolean = false, val error: AuthError? = null)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val uiState = mutableState.asStateFlow()
    fun email(value: String) { if (!mutableState.value.busy) mutableState.value = mutableState.value.copy(email = value, error = null) }
    fun password(value: String) { if (!mutableState.value.busy) mutableState.value = mutableState.value.copy(password = value, error = null) }
    fun toggleMode() { if (!mutableState.value.busy) mutableState.value = mutableState.value.copy(register = !mutableState.value.register, password = "", error = null) }
    fun clearCredentials() { mutableState.value = AuthUiState() }
    fun submit() {
        val state = mutableState.value
        if (state.busy) return
        if (!repository.configured) { mutableState.value = state.copy(error = AuthError.NOT_CONFIGURED); return }
        if (!state.email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) || state.password.isEmpty() || (state.register && state.password.length < 6)) {
            mutableState.value = state.copy(error = AuthError.INVALID_INPUT); return
        }
        mutableState.value = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                if (state.register) repository.signUp(state.email, state.password)
                else repository.signIn(state.email, state.password)
                clearCredentials()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.copy(password = "", error = AuthError.FAILED) }
        }
    }
    companion object { fun factory(repository: AuthRepository) = viewModelFactory { initializer { AuthViewModel(repository) } } }
}
