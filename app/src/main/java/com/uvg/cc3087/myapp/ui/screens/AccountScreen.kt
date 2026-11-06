package com.uvg.cc3087.myapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(session: UserSession?, configured: Boolean, auth: AuthUiState,
    account: AccountUiState, guests: List<FormSummary>, syncMessage: String?,
    onEmail: (String) -> Unit, onPassword: (String) -> Unit, onToggle: () -> Unit,
    onSubmit: () -> Unit, onBack: () -> Unit, onSelect: (String) -> Unit,
    onImport: () -> Unit, onSignOut: () -> Unit, onSync: () -> Unit) {
    BackHandler { if (!auth.busy && !account.busy) onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.account_title)) },
        navigationIcon = { TextButton(onClick = onBack, enabled = !auth.busy && !account.busy) { Text(stringResource(R.string.go_back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (session == null) {
                Text(stringResource(if (auth.register) R.string.auth_register else R.string.auth_sign_in), style = MaterialTheme.typography.headlineSmall)
                if (!configured) Text(stringResource(R.string.backend_not_configured))
                OutlinedTextField(auth.email, onEmail, enabled = !auth.busy, singleLine = true,
                    label = { Text(stringResource(R.string.auth_email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(auth.password, onPassword, enabled = !auth.busy, singleLine = true,
                    label = { Text(stringResource(R.string.auth_password)) }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                if (auth.error != null) Text(stringResource(when (auth.error) {
                    AuthError.INVALID_INPUT -> R.string.auth_invalid
                    AuthError.NOT_CONFIGURED -> R.string.backend_not_configured
                    AuthError.FAILED -> R.string.auth_failed
                }), color = MaterialTheme.colorScheme.error)
                if (auth.busy) CircularProgressIndicator()
                Button(onClick = onSubmit, enabled = configured && !auth.busy) {
                    Text(stringResource(if (auth.register) R.string.auth_register else R.string.auth_sign_in))
                }
                TextButton(onClick = onToggle, enabled = !auth.busy) {
                    Text(stringResource(if (auth.register) R.string.auth_have_account else R.string.auth_need_account))
                }
                OutlinedButton(onClick = onBack, enabled = !auth.busy) { Text(stringResource(R.string.auth_continue_guest)) }
            } else {
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
}
