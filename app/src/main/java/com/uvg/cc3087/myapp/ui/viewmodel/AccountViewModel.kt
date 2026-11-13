package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountUiState(val busy: Boolean = false, val failed: Boolean = false)
class AccountViewModel(private val auth: AuthRepository, local: LocalFormStore,
    private val forms: OfflineFirstFormRepository, private val imports: GuestImportRepository,
    private val cancel: (String) -> Unit) : ViewModel() {
    val importState = (forms.owner?.let(imports::observe) ?: flowOf(GuestImportState()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GuestImportState())
    val pending = forms.owner?.let { local.pendingCount(it) }?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    private val mutableState = MutableStateFlow(AccountUiState())
    val uiState = mutableState.asStateFlow()
    fun retryImport() {
        if (mutableState.value.busy) return
        forms.owner?.let { owner -> viewModelScope.launch { imports.resume(owner) } }
    }
    fun signOut() {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, failed = false)
        viewModelScope.launch {
            try { forms.owner?.let(cancel); auth.signOut() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(busy = false, failed = true) }
        }
    }
    companion object {
        fun factory(auth: AuthRepository, local: LocalFormStore, forms: OfflineFirstFormRepository,
            imports: GuestImportRepository, cancel: (String) -> Unit) =
            viewModelFactory { initializer { AccountViewModel(auth, local, forms, imports, cancel) } }
    }
}
