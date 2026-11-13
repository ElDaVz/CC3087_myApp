package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.repository.GuestImportPhase
import com.uvg.cc3087.myapp.data.repository.GuestImportState

@Composable
fun GuestImportNotice(state: GuestImportState, onRetry: () -> Unit) {
    if (state.phase == GuestImportPhase.NONE) return
    val running = state.phase == GuestImportPhase.PENDING || state.phase == GuestImportPhase.IMPORTING
    Surface(modifier = Modifier.fillMaxWidth().testTag("guest-import-notice")
        .semantics { liveRegion = LiveRegionMode.Polite }, shape = MaterialTheme.shapes.medium,
        color = if (state.phase == GuestImportPhase.ERROR) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(when (state.phase) {
                GuestImportPhase.COMPLETE -> R.string.guest_import_complete
                GuestImportPhase.ERROR -> R.string.guest_import_error
                else -> R.string.guest_import_running
            }, state.count))
            if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.phase == GuestImportPhase.ERROR) OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.guest_import_retry))
            }
        }
    }
}
