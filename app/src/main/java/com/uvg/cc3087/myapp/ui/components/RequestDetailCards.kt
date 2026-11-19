package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.RequestAnswerValue
import com.uvg.cc3087.myapp.data.model.RequestDetail
import com.uvg.cc3087.myapp.data.model.RequestDetailAnswer
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.ui.resources.labelRes
import com.uvg.cc3087.myapp.ui.state.RequestDetailUiState

@Composable
internal fun RequestMetadataCard(detail: RequestDetail) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(detail.formTitle.ifBlank { stringResource(R.string.untitled_form) },
                style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            RequestStatusBadge(detail.requestStatus, detail.statusLabel)
            Text(stringResource(R.string.request_detail_submitted, requestDate(detail.submittedAt)),
                style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            SelectionContainer {
                // el id completo queda disponible aquí, aunque la tarjeta de la lista lo abrevie visualmente
                Text(stringResource(R.string.request_detail_id, detail.responseId),
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
internal fun RequestReadOnlyNotice(canChangeStatus: Boolean) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(stringResource(if (canChangeStatus) R.string.request_answers_read_only else
                R.string.request_answers_read_only_no_status), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RequestStatusControls(state: RequestDetailUiState, onChange: (RequestStatus) -> Unit, onRetry: () -> Unit) {
    val current = state.detail?.requestStatus ?: return
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.request_change_status), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RequestStatus.entries.forEach { status ->
                    FilterChip(selected = current == status, enabled = !state.savingStatus,
                        onClick = { onChange(status) }, modifier = Modifier.testTag("detail-status-${status.name}"),
                        label = { Text(stringResource(status.labelRes)) })
                }
            }
            if (state.savingStatus) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.request_status_saving), style = MaterialTheme.typography.labelMedium)
            }
            if (state.statusSaveFailed) {
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val failed = state.failedStatus
                    Text(if (failed == null) stringResource(R.string.request_status_error) else
                        stringResource(R.string.request_status_failed_target, stringResource(failed.labelRes)),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    if (failed != null) {
                        OutlinedButton(onClick = onRetry, enabled = !state.savingStatus,
                            modifier = Modifier.testTag("retry-request-status")) {
                            Text(stringResource(R.string.request_status_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RequestAnswerCard(answer: RequestDetailAnswer, index: Int) {
    OutlinedCard(Modifier.fillMaxWidth().testTag("received-answer-$index")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.request_question_number, index + 1),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(answer.label.ifBlank { stringResource(R.string.request_detail_unnamed_field) },
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            SelectionContainer { RequestAnswerContent(answer.value) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestAnswerContent(value: RequestAnswerValue) {
    val noAnswer = stringResource(R.string.request_detail_no_answer)
    when (value) {
        // no intentamos adivinar si un texto histórico era número, fecha o correo
        is RequestAnswerValue.Text -> Text(value.value.ifBlank { noAnswer }, style = MaterialTheme.typography.bodyLarge)
        is RequestAnswerValue.Checked -> Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (value.value) Icons.Outlined.CheckCircleOutline else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(if (value.value) R.string.request_detail_yes else R.string.request_detail_no))
        }
        is RequestAnswerValue.Choices -> {
            val labels = value.labels.filter { it.isNotBlank() }
            if (labels.isEmpty()) Text(noAnswer) else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { label ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
        is RequestAnswerValue.Attachments -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (value.files.isEmpty()) Text(noAnswer) else {
                value.files.forEach { file ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null)
                        Text(file.name.ifBlank { stringResource(R.string.request_detail_attachment) })
                    }
                }
                Text(stringResource(R.string.request_detail_attachment_read_only), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
