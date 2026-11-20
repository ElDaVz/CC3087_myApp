package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.di.BackendServices
import com.uvg.cc3087.myapp.ui.screens.AccountScreen
import com.uvg.cc3087.myapp.ui.screens.AuthScreen
import com.uvg.cc3087.myapp.ui.viewmodel.*
import com.uvg.cc3087.myapp.ui.components.GuestImportNotice
import com.uvg.cc3087.myapp.ui.components.accountSyncMessage

@Composable
internal fun BackendRoot(services: BackendServices, templates: TemplateRepository,
    requests: RequestRepository, covers: CoverStorage) {
    val authState by services.auth.state.collectAsStateWithLifecycle()
    val authModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(services.auth, services.googleSignIn))
    val authUi by authModel.uiState.collectAsStateWithLifecycle()
    val uid = authState.userId
    LaunchedEffect(authState) {
        if (authState !is AuthState.Initializing) authModel.clearCredentials()
    }
    if (authState is AuthState.Initializing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    key(uid) {
        val owner = rememberSaveable(saver = Saver<SessionViewModelOwner, android.os.Bundle>(
            save = { android.os.Bundle().apply { putByteArray("encrypted", services.deviceCipher.encrypt(it.save().toBytes())) } },
            restore = { SessionViewModelOwner(it.getByteArray("encrypted")?.let { bytes -> androidBundle(services.deviceCipher.decrypt(bytes)) }) })) { SessionViewModelOwner() }
        val forms = remember(uid) { services.repository(uid) }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(uid, lifecycleOwner) {
            if (uid != null) lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    services.schedule(uid)
                    kotlinx.coroutines.delay(15_000)
                }
            }
        }
        DisposableEffect(owner) { onDispose { owner.clear() } }
        LaunchedEffect(uid) { uid?.let(services::schedule) }
        DisposableEffect(uid, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) uid?.let(services::schedule)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                if (services.auth.state.value.userId != uid) uid?.let(services::cancel)
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            val accountModel: AccountViewModel = viewModel(factory = AccountViewModel.factory(services.auth, uid,
                services.accountSync, services.guestImports, services::cancel, services.profiles))
            val accountUi by accountModel.uiState.collectAsStateWithLifecycle()
            val importState by accountModel.importState.collectAsStateWithLifecycle()
            val syncState by accountModel.syncState.collectAsStateWithLifecycle()
            val profile by accountModel.profile.collectAsStateWithLifecycle()
            val message = if (uid == null) null else accountSyncMessage(syncState)
            val responseRepository = remember(uid) { if (uid == null) requests else services.requests(uid) ?: EmptyAccountRequests }
            FormLinkRoute(forms, templates, responseRepository,
                guestImportNotice = { GuestImportNotice(importState, accountModel::retryImport) },
                analytics = services.analytics, coverStorage = covers, syncMessage = message,
                businessLogoPath = profile.logoPath,
                ownerId = uid ?: "guest", formListPreferences = services.formListPreferences,
                syncState = if (uid == null) null else syncState,
                onSyncNow = if (uid == null) null else accountModel::requestSync,
                accountContent = { goBack ->
                    if (uid == null) AuthScreen(authUi, services.auth.configured,
                        authModel::email, authModel::password, authModel::confirmation, authModel::mode, authModel::submit,
                        googleAvailable = authModel.googleAvailable, onGoogle = authModel::googleSignIn,
                        onBack = {
                            if (authUi.mode != AuthMode.LOGIN) authModel.mode(AuthMode.LOGIN)
                            else { goBack(); authModel.clearCredentials() }
                        })
                    else AccountScreen((authState as AuthState.SignedIn).session,
                        accountUi, importState, syncState, onBack = goBack,
                        onRetryImport = accountModel::retryImport,
                        onSignOut = accountModel::signOut, onSync = accountModel::requestSync,
                        onConfirmSignOut = accountModel::confirmSignOut, onDismissSignOut = accountModel::dismissSignOut,
                        profile = profile, onProfileChange = accountModel::updateProfile,
                        onLogoPick = accountModel::importLogo, onLogoRemove = accountModel::removeLogo)
                })
        }
    }
}

// Existing request snapshots belong to the installation's guest partition, not to signed-in accounts.
private object EmptyAccountRequests : RequestRepository {
    override suspend fun getRequests() = emptyList<RequestSummary>()
    override suspend fun getDetail(formId: String, responseId: String): RequestDetail? = null
}

private fun android.os.Bundle.toBytes(): ByteArray = android.os.Parcel.obtain().let { parcel ->
    try { parcel.writeBundle(this); parcel.marshall() } finally { parcel.recycle() }
}
private fun androidBundle(bytes: ByteArray): android.os.Bundle? = android.os.Parcel.obtain().let { parcel ->
    try { parcel.unmarshall(bytes, 0, bytes.size); parcel.setDataPosition(0); parcel.readBundle(SessionViewModelOwner::class.java.classLoader) }
    finally { parcel.recycle() }
}
