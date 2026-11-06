package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountUiState(val selected: Set<String> = emptySet(), val busy: Boolean = false, val failed: Boolean = false)
class AccountViewModel(private val auth: AuthRepository, local: LocalFormStore,
    private val forms: OfflineFirstFormRepository, private val cancel: (String) -> Unit) : ViewModel() {
    val guests = local.observe(null).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pending = forms.owner?.let { local.pendingCount(it) }?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    private val mutableState = MutableStateFlow(AccountUiState())
    val uiState = mutableState.asStateFlow()
    fun select(id: String) {
        val state = mutableState.value
        if (!state.busy) mutableState.value = state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id)
    }
    fun importSelected() {
        val state = mutableState.value
        if (state.busy || state.selected.isEmpty()) return
        mutableState.value = state.copy(busy = true, failed = false)
        viewModelScope.launch {
            try { forms.importGuests(state.selected); mutableState.value = AccountUiState() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.copy(failed = true) }
        }
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
        fun factory(auth: AuthRepository, local: LocalFormStore, forms: OfflineFirstFormRepository, cancel: (String) -> Unit) =
            viewModelFactory { initializer { AccountViewModel(auth, local, forms, cancel) } }
    }
}
