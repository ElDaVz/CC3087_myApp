package com.uvg.cc3087.myapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.repository.AccountSyncPhase
import com.uvg.cc3087.myapp.data.repository.AccountSyncState

@Composable
fun accountSyncMessage(state: AccountSyncState): String = stringResource(when (state.phase) {
    AccountSyncPhase.UNCHECKED -> R.string.account_sync_unchecked
    AccountSyncPhase.QUEUED -> R.string.account_sync_queued
    AccountSyncPhase.WAITING_CONNECTION -> R.string.account_sync_offline
    AccountSyncPhase.SYNCING -> R.string.sync_running
    AccountSyncPhase.UP_TO_DATE -> R.string.account_sync_checked
    AccountSyncPhase.ERROR -> R.string.sync_error
})
