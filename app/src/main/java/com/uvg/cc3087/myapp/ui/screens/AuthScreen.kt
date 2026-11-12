package com.uvg.cc3087.myapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.repository.AuthFailure
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(state: AuthUiState, configured: Boolean,
    onEmail: (String) -> Unit, onPassword: (String) -> Unit, onConfirmation: (String) -> Unit,
    onMode: (AuthMode) -> Unit, onSubmit: () -> Unit, onBack: () -> Unit) {
    BackHandler { if (!state.busy) onBack() }
    // The authentication surface uses the brand palette without changing the app theme.
    MyappTheme(dynamicColor = false) {
        val title = stringResource(when (state.mode) {
            AuthMode.LOGIN -> R.string.auth_sign_in
            AuthMode.REGISTER -> R.string.auth_register
            AuthMode.RECOVERY -> R.string.auth_recovery
        })
        Scaffold(topBar = { TopAppBar(title = { Text(if (state.recoverySent) "" else title) },
            navigationIcon = { TextButton(onClick = onBack, enabled = !state.busy) { Text(stringResource(R.string.go_back)) } }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.recoverySent) {
                        Text(stringResource(R.string.auth_check_email), style = MaterialTheme.typography.headlineSmall)
                    } else {
                        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null,
                            modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                        if (!configured || state.unavailable) Text(stringResource(R.string.backend_not_configured))
                        OutlinedTextField(state.email, onEmail, Modifier.fillMaxWidth(), enabled = !state.busy,
                            singleLine = true, label = { Text(stringResource(R.string.auth_email)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = state.emailError != null, supportingText = state.emailError?.let { { FieldError(it) } })
                        if (state.mode != AuthMode.RECOVERY) {
                            PasswordField(state.password, onPassword, R.string.auth_password, state.passwordError, !state.busy, state.mode)
                            if (state.mode == AuthMode.REGISTER) PasswordField(state.confirmation, onConfirmation,
                                R.string.auth_confirm_password, state.confirmationError, !state.busy, state.mode)
                            if (state.mode == AuthMode.LOGIN) TextButton(onClick = { onMode(AuthMode.RECOVERY) },
                                enabled = !state.busy, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.auth_forgot_password), fontStyle = FontStyle.Italic, fontSize = 14.sp)
                            }
                        }
                        state.error?.let { Text(stringResource(when (it) {
                            AuthFailure.INVALID_CREDENTIALS -> R.string.auth_invalid_credentials
                            AuthFailure.EMAIL_EXISTS -> R.string.auth_email_exists
                            AuthFailure.INVALID_EMAIL -> R.string.auth_email_format
                            AuthFailure.WEAK_PASSWORD -> R.string.auth_password_length
                            AuthFailure.DISABLED -> R.string.auth_disabled
                            AuthFailure.TOO_MANY_ATTEMPTS -> R.string.auth_too_many_attempts
                            AuthFailure.NETWORK -> R.string.auth_network
                            AuthFailure.UNEXPECTED -> R.string.auth_unexpected
                        }), color = MaterialTheme.colorScheme.error) }
                        if (state.busy) CircularProgressIndicator()
                        Button(onClick = onSubmit, enabled = configured && !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.mode == AuthMode.RECOVERY) stringResource(R.string.auth_send_link) else title)
                        }
                        if (state.mode != AuthMode.RECOVERY) TextButton(onClick = {
                            onMode(if (state.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN)
                        }, enabled = !state.busy) {
                            Text(stringResource(if (state.mode == AuthMode.LOGIN) R.string.auth_need_account else R.string.auth_have_account))
                        }
                        if (state.mode == AuthMode.LOGIN) OutlinedButton(onClick = onBack, enabled = !state.busy) {
                            Text(stringResource(R.string.auth_continue_guest))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(value: String, onValue: (String) -> Unit, label: Int,
    error: AuthFieldError?, enabled: Boolean, mode: AuthMode) {
    var visible by remember(mode) { mutableStateOf(false) }
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
        label = { Text(stringResource(label)) }, isError = error != null,
        supportingText = error?.let { { FieldError(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = enabled) {
            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                stringResource(if (visible) R.string.auth_hide_password else R.string.auth_show_password))
        } })
}

@Composable
private fun FieldError(error: AuthFieldError) {
    Text(stringResource(when (error) {
        AuthFieldError.REQUIRED -> R.string.auth_required
        AuthFieldError.EMAIL_FORMAT -> R.string.auth_email_format
        AuthFieldError.PASSWORD_LENGTH -> R.string.auth_password_length
        AuthFieldError.PASSWORD_MISMATCH -> R.string.auth_password_mismatch
    }))
}
