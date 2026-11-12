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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(session: UserSession,
    account: AccountUiState, guests: List<FormSummary>, syncMessage: String?,
    onBack: () -> Unit, onSelect: (String) -> Unit,
    onImport: () -> Unit, onSignOut: () -> Unit, onSync: () -> Unit) {
    BackHandler { if (!account.busy) onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.account_title)) },
        navigationIcon = { TextButton(onClick = onBack, enabled = !account.busy) { Text(stringResource(R.string.go_back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(session.email, style = MaterialTheme.typography.titleLarge)
            syncMessage?.let { Text(it) }
            OutlinedButton(onClick = onSync, enabled = !account.busy) { Text(stringResource(R.string.sync_retry)) }
            Text(stringResource(R.string.import_guest_notice))
            guests.forEach { form ->
                Row(Modifier.fillMaxWidth()) {
                    Checkbox(checked = form.id in account.selected, onCheckedChange = { onSelect(form.id) }, enabled = !account.busy)
                    Text(form.title.ifBlank { stringResource(R.string.untitled_form) }, Modifier.padding(top = 12.dp))
                }
            }
            if (account.failed) Text(stringResource(R.string.account_failed), color = MaterialTheme.colorScheme.error)
            if (account.busy) CircularProgressIndicator()
            Button(onClick = onImport, enabled = !account.busy && account.selected.isNotEmpty()) { Text(stringResource(R.string.import_selected)) }
            OutlinedButton(onClick = onSignOut, enabled = !account.busy) { Text(stringResource(R.string.auth_sign_out)) }
        }
    }
}
