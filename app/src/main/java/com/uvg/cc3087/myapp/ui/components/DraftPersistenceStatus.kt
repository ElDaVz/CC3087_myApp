package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.ui.state.DraftLoadError
import com.uvg.cc3087.myapp.ui.state.DraftSaveStatus

@Composable
fun DraftSaveNotice(status: DraftSaveStatus, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (status == DraftSaveStatus.IDLE) return
    val message = when (status) {
        DraftSaveStatus.SAVING -> R.string.draft_saving
        DraftSaveStatus.SAVED -> R.string.draft_saved
        DraftSaveStatus.ERROR -> R.string.draft_save_error
        DraftSaveStatus.IDLE -> return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(message),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (status == DraftSaveStatus.ERROR) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (status == DraftSaveStatus.ERROR) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun DraftLoadStatus(error: DraftLoadError?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (error == null) CircularProgressIndicator()
        Text(stringResource(when (error) {
            null -> R.string.draft_loading
            DraftLoadError.NOT_FOUND -> R.string.draft_not_found
            DraftLoadError.FAILED -> R.string.draft_load_error
        }))
        if (error != null) {
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
