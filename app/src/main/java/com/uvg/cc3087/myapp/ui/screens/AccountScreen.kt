package com.uvg.cc3087.myapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.UserSession
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.ui.components.GuestImportNotice
import com.uvg.cc3087.myapp.ui.components.accountSyncMessage
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.AccountUiState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(session: UserSession,
    account: AccountUiState, importState: GuestImportState, syncState: AccountSyncState,
    onBack: () -> Unit, onRetryImport: () -> Unit, onSignOut: () -> Unit, onSync: () -> Unit,
    onConfirmSignOut: () -> Unit = {}, onDismissSignOut: () -> Unit = {}) {
    BackHandler { if (!account.busy) onBack() }
    MyappTheme(dynamicColor = false) {
        if (account.confirmSignOut) AlertDialog(onDismissRequest = onDismissSignOut,
            title = { Text(stringResource(R.string.account_sign_out_title)) },
            text = { Text(stringResource(R.string.account_sign_out_pending)) },
            confirmButton = { TextButton(onClick = onConfirmSignOut) { Text(stringResource(R.string.auth_sign_out)) } },
            dismissButton = { TextButton(onClick = onDismissSignOut) { Text(stringResource(R.string.account_keep_session)) } })
        Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.account_title)) },
            navigationIcon = { IconButton(onClick = onBack, enabled = !account.busy) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
            } }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(R.drawable.ic_launcher_foreground), null,
                                Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)))
                            Text(stringResource(R.string.account_identity), style = MaterialTheme.typography.labelLarge)
                            Text(session.email, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.CloudSync, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.account_sync_title), style = MaterialTheme.typography.titleLarge)
                            Text(accountSyncMessage(syncState), style = MaterialTheme.typography.titleMedium,
                                color = if (syncState.phase == AccountSyncPhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.account_pending_drafts, syncState.pending.drafts))
                            Text(stringResource(R.string.account_pending_deletions, syncState.pending.deletions))
                            Text(stringResource(R.string.account_pending_responses, syncState.pending.responseStatuses))
                            syncState.lastSuccessfulCheck?.let {
                                Text(stringResource(R.string.account_last_check, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))))
                            }
                            Text(stringResource(R.string.account_private_images), style = MaterialTheme.typography.bodySmall)
                            if (syncState.phase == AccountSyncPhase.SYNCING) LinearProgressIndicator(Modifier.fillMaxWidth())
                            OutlinedButton(onClick = onSync, enabled = !account.busy && syncState.phase !in setOf(AccountSyncPhase.SYNCING, AccountSyncPhase.WAITING_CONNECTION)) {
                                Text(stringResource(when {
                                    syncState.phase == AccountSyncPhase.ERROR -> R.string.sync_retry
                                    syncState.pending.total > 0 -> R.string.account_sync_now
                                    else -> R.string.account_sync_check
                                }))
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.FolderCopy, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.account_import_title), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.account_import_local), style = MaterialTheme.typography.bodySmall)
                            if (importState.phase == GuestImportPhase.NONE) Text(stringResource(R.string.account_import_empty))
                            GuestImportNotice(importState, onRetryImport)
                        }
                    }
                    Text(stringResource(R.string.account_local_retained), style = MaterialTheme.typography.bodySmall)
                    if (account.failed) Text(stringResource(R.string.account_failed), color = MaterialTheme.colorScheme.error)
                    if (account.busy) CircularProgressIndicator()
                    OutlinedButton(onClick = onSignOut, enabled = !account.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.auth_sign_out))
                    }
                }
            }
        }
    }
}
