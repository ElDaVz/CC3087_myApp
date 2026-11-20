package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.OwnerProfile
import com.uvg.cc3087.myapp.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AccountUiState(val busy: Boolean = false, val failed: Boolean = false, val confirmSignOut: Boolean = false)
class AccountViewModel(private val auth: AuthRepository, private val owner: String?,
    private val sync: AccountSyncRepository, private val imports: GuestImportRepository,
    private val cancel: (String) -> Unit, private val profiles: ProfileRepository? = null) : ViewModel() {
    val profile: StateFlow<OwnerProfile> = (owner?.let { id -> profiles?.observe(id) }
        ?: flowOf(OwnerProfile(owner.orEmpty())))
        .stateIn(viewModelScope, SharingStarted.Eagerly, OwnerProfile(owner.orEmpty()))
    val importState = (owner?.let(imports::observe) ?: flowOf(GuestImportState()))
        .stateIn(viewModelScope, SharingStarted.Eagerly, GuestImportState())
    val syncState = (owner?.let(sync::observe) ?: flowOf(AccountSyncState()))
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountSyncState())
    private val mutableState = MutableStateFlow(AccountUiState())
    private val profileWrites = Mutex()
    val uiState = mutableState.asStateFlow()
    fun requestSync() { if (!uiState.value.busy) owner?.let(sync::requestSync) }

    /** Profile edits are saved locally first; sync is scheduled by the repository pass. */
    fun updateProfile(change: (OwnerProfile) -> OwnerProfile) {
        val id = owner ?: return
        val repository = profiles ?: return
        viewModelScope.launch {
            profileWrites.withLock {
                try { repository.save(id, change(repository.get(id))) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutableState.value = uiState.value.copy(failed = true) }
            }
        }
    }

    fun importLogo(uri: String) {
        val id = owner ?: return
        val repository = profiles ?: return
        mutableState.value = uiState.value.copy(busy = true, failed = false)
        viewModelScope.launch {
            profileWrites.withLock {
                try { repository.importLogo(id, uri) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutableState.value = uiState.value.copy(failed = true) }
                finally { mutableState.value = mutableState.value.copy(busy = false) }
            }
        }
    }

    fun removeLogo() {
        val id = owner ?: return
        val repository = profiles ?: return
        viewModelScope.launch {
            profileWrites.withLock {
                try { repository.removeLogo(id) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutableState.value = uiState.value.copy(failed = true) }
            }
        }
    }
    fun retryImport() {
        if (uiState.value.busy) return
        owner?.let { id -> viewModelScope.launch { imports.resume(id) } }
    }
    fun signOut() {
        if (uiState.value.busy) return
        val pendingImport = importState.value.phase in setOf(GuestImportPhase.PENDING, GuestImportPhase.IMPORTING, GuestImportPhase.ERROR)
        if (syncState.value.pending.total > 0 || syncState.value.phase in setOf(AccountSyncPhase.SYNCING, AccountSyncPhase.QUEUED) || pendingImport) {
            mutableState.value = uiState.value.copy(confirmSignOut = true)
        } else confirmSignOut()
    }
    fun dismissSignOut() { mutableState.value = uiState.value.copy(confirmSignOut = false) }
    fun confirmSignOut() {
        if (uiState.value.busy) return
        mutableState.value = AccountUiState(busy = true)
        viewModelScope.launch {
            try { owner?.let(cancel); auth.signOut() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = AccountUiState(failed = true) }
        }
    }
    companion object {
        fun factory(auth: AuthRepository, owner: String?, sync: AccountSyncRepository,
            imports: GuestImportRepository, cancel: (String) -> Unit, profiles: ProfileRepository? = null) =
            viewModelFactory { initializer { AccountViewModel(auth, owner, sync, imports, cancel, profiles) } }
    }
}
