package com.uvg.cc3087.myapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.ui.viewmodel.*
import com.uvg.cc3087.myapp.data.repository.GuestImportState
import com.uvg.cc3087.myapp.ui.components.GuestImportNotice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(session: UserSession,
    account: AccountUiState, importState: GuestImportState, syncMessage: String?,
    onBack: () -> Unit, onRetryImport: () -> Unit, onSignOut: () -> Unit, onSync: () -> Unit) {
    BackHandler { if (!account.busy) onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.account_title)) },
        navigationIcon = { TextButton(onClick = onBack, enabled = !account.busy) { Text(stringResource(R.string.go_back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(session.email, style = MaterialTheme.typography.titleLarge)
            syncMessage?.let { Text(it) }
            OutlinedButton(onClick = onSync, enabled = !account.busy) { Text(stringResource(R.string.sync_retry)) }
            GuestImportNotice(importState, onRetryImport)
            if (account.failed) Text(stringResource(R.string.account_failed), color = MaterialTheme.colorScheme.error)
            if (account.busy) CircularProgressIndicator()
            OutlinedButton(onClick = onSignOut, enabled = !account.busy) { Text(stringResource(R.string.auth_sign_out)) }
        }
    }
}
