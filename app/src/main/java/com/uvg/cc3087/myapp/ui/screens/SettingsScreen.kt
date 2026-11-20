package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.ThemeMode
import com.uvg.cc3087.myapp.ui.viewmodel.AppearanceUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    appearance: AppearanceUiState,
    onTheme: (ThemeMode) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onAccount: (() -> Unit)?,
    onStatistics: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings)) }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
            }
        })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.settings_shortcuts), Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge)
                        if (onAccount != null) OutlinedButton(onClick = onAccount, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.account_title))
                        }
                        OutlinedButton(onClick = onStatistics, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_statistics))
                        }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.settings_appearance), Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.settings_appearance_device), style = MaterialTheme.typography.bodyMedium)
                        Column(Modifier.fillMaxWidth().selectableGroup()) {
                            ThemeMode.entries.forEach { mode ->
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                        selected = mode == appearance.mode,
                                        enabled = appearance.loaded && !appearance.saving,
                                        role = Role.RadioButton,
                                        onClick = { onTheme(mode) }
                                    ).padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(selected = mode == appearance.mode, onClick = null,
                                        enabled = appearance.loaded && !appearance.saving)
                                    Text(stringResource(when (mode) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    }))
                                }
                            }
                        }
                        if (!appearance.loaded || appearance.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (appearance.failed) {
                            Text(stringResource(if (appearance.failedMode != null) R.string.theme_save_error else R.string.theme_read_error),
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = onRetry, enabled = !appearance.saving) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}
