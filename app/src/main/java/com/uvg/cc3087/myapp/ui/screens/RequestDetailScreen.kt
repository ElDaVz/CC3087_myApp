package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.ui.components.RequestAnswerCard
import com.uvg.cc3087.myapp.ui.components.RequestMetadataCard
import com.uvg.cc3087.myapp.ui.components.RequestReadOnlyNotice
import com.uvg.cc3087.myapp.ui.components.RequestStatusControls
import com.uvg.cc3087.myapp.ui.state.RequestDetailLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestDetailUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    uiState: RequestDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isDemo: Boolean = false,
    onStatusChange: (RequestStatus) -> Unit = {},
    onRetryRead: () -> Unit = {},
    onRetryStatus: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.request_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().testTag("request-detail-list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (isDemo) item("demo") {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                        Text(stringResource(R.string.request_detail_demo), Modifier.fillMaxWidth().padding(16.dp))
                    }
                }
                val detail = uiState.detail
                if (uiState.status == RequestDetailLoadStatus.CONTENT && detail != null) {
                    item("metadata") { RequestMetadataCard(detail) }
                    item("read-only") { RequestReadOnlyNotice(canChangeStatus = detail.requestStatus != null) }
                    if (uiState.readSaveFailed) item("read-error") {
                        OutlinedCard {
                            Column(Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.dashboard_read_error), color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = onRetryRead, enabled = !uiState.markingRead) {
                                    Text(stringResource(R.string.dashboard_retry_read))
                                }
                            }
                        }
                    }
                    if (detail.requestStatus != null) item("status-controls") {
                        RequestStatusControls(uiState, onStatusChange, onRetryStatus)
                    }
                    item("answers-heading") {
                        Text(stringResource(R.string.request_detail_answers), style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() })
                    }
                    if (detail.answers.isEmpty()) item("no-answers") {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.request_detail_no_answers), Modifier.padding(20.dp))
                        }
                    }
                    itemsIndexed(detail.answers, key = { index, answer -> "$index/${answer.fieldId}" }) { index, answer ->
                        RequestAnswerCard(answer, index)
                    }
                } else {
                    item("load-status") {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(24.dp).semantics { liveRegion = LiveRegionMode.Polite },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (uiState.status == RequestDetailLoadStatus.LOADING) CircularProgressIndicator()
                                else Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(when (uiState.status) {
                                    RequestDetailLoadStatus.IDLE -> R.string.request_detail_select
                                    RequestDetailLoadStatus.LOADING -> R.string.request_detail_loading
                                    RequestDetailLoadStatus.NOT_FOUND -> R.string.request_detail_not_found
                                    RequestDetailLoadStatus.FORBIDDEN -> R.string.request_detail_forbidden
                                    RequestDetailLoadStatus.UNAVAILABLE -> R.string.requests_unavailable
                                    else -> R.string.request_detail_error
                                }))
                                if (uiState.status == RequestDetailLoadStatus.ERROR || uiState.status == RequestDetailLoadStatus.FORBIDDEN) {
                                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "es")
@Composable
private fun RequestDetailEmptyPreview() {
    MyappTheme(dynamicColor = false) {
        RequestDetailScreen(RequestDetailUiState(RequestDetailLoadStatus.NOT_FOUND), {}, {})
    }
}
