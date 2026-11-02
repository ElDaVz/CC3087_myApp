package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestAnswerValue
import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.ui.resources.labelRes
import androidx.compose.material3.FilterChip
import com.uvg.cc3087.myapp.ui.state.RequestDetailLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestDetailUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Submitted answers remain read-only; local request status can be updated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    uiState: RequestDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isDemo: Boolean = false,
    onStatusChange: (RequestStatus) -> Unit = {}
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isDemo) item {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.request_detail_demo), Modifier.fillMaxWidth().padding(16.dp))
                }
            }
            val detail = uiState.detail
            if (uiState.status == RequestDetailLoadStatus.CONTENT && detail != null) {
                item { RequestMetadata(detail) }
                if (detail.requestStatus != null) item {
                    Column {
                        Text(stringResource(R.string.request_change_status))
                        RequestStatus.entries.forEach { status ->
                            FilterChip(selected = detail.requestStatus == status,
                                enabled = !uiState.savingStatus,
                                onClick = { onStatusChange(status) },
                                label = { Text(stringResource(status.labelRes)) })
                        }
                        if (uiState.savingStatus) CircularProgressIndicator()
                        if (uiState.statusSaveFailed) Text(stringResource(R.string.request_status_error),
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    Text(
                        stringResource(R.string.request_detail_answers),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                if (detail.answers.isEmpty()) item { Text(stringResource(R.string.request_detail_no_answers)) }
                itemsIndexed(detail.answers) { _, answer ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                answer.label.ifBlank { stringResource(R.string.request_detail_unnamed_field) },
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() }
                            )
                            SelectionContainer { RequestAnswerContent(answer.value) }
                        }
                    }
                }
            } else item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.status == RequestDetailLoadStatus.LOADING) CircularProgressIndicator()
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

@Composable
private fun RequestMetadata(detail: RequestDetail) {
    val locale = LocalResources.current.configuration.locales[0]
    val submitted = detail.submittedAt?.let {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
            .withZone(ZoneId.systemDefault()).format(it)
    } ?: stringResource(R.string.request_detail_no_date)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                detail.formTitle.ifBlank { stringResource(R.string.untitled_form) },
                style = MaterialTheme.typography.headlineSmall
            )
            SelectionContainer {
                Text(stringResource(R.string.request_detail_id, detail.responseId))
            }
            Text(stringResource(R.string.request_detail_submitted, submitted))
            Text(stringResource(R.string.request_detail_status,
                detail.requestStatus?.let { stringResource(it.labelRes) }
                    ?: detail.statusLabel?.takeIf { it.isNotBlank() } ?: stringResource(R.string.request_detail_no_status)))
        }
    }
}

@Composable
private fun RequestAnswerContent(value: RequestAnswerValue) {
    val noAnswer = stringResource(R.string.request_detail_no_answer)
    when (value) {
        is RequestAnswerValue.Text -> Text(value.value.ifBlank { noAnswer })
        is RequestAnswerValue.Checked -> Text(stringResource(
            if (value.value) R.string.request_detail_yes else R.string.request_detail_no
        ))
        is RequestAnswerValue.Choices -> {
            val labels = value.labels.filter { it.isNotBlank() }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (labels.isEmpty()) Text(noAnswer)
                else labels.forEach { Text("• $it") }
            }
        }
        is RequestAnswerValue.Attachments -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (value.files.isEmpty()) Text(noAnswer)
            else {
                value.files.forEach { Text(it.name.ifBlank { stringResource(R.string.request_detail_attachment) }) }
                Text(stringResource(R.string.request_detail_attachment_read_only), style = MaterialTheme.typography.bodySmall)
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
