package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.domain.requests.*
import com.uvg.cc3087.myapp.ui.resources.labelRes
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardUiState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ResponseFilters(filter: ResponseFilter, forms: List<ResponseForm>, onForm: (String?) -> Unit, onPeriod: (ResponsePeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag("dashboard-form-filter")) {
                Text(forms.find { it.id == filter.formId }?.let { formLabel(it, forms) } ?: stringResource(R.string.dashboard_all_forms))
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.dashboard_all_forms)) }, onClick = { onForm(null); expanded = false })
                forms.forEach { form -> DropdownMenuItem(text = { Text(formLabel(form, forms)) }, onClick = { onForm(form.id); expanded = false }) }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResponsePeriod.entries.forEach { period ->
                FilterChip(selected = filter.period == period, onClick = { onPeriod(period) }, modifier = Modifier.testTag("period-${period.name}"), label = {
                    Text(stringResource(when (period) { ResponsePeriod.WEEK -> R.string.dashboard_week; ResponsePeriod.MONTH -> R.string.dashboard_month; ResponsePeriod.ALL -> R.string.dashboard_all_time }))
                })
            }
        }
    }
}

@Composable
private fun formLabel(form: ResponseForm, forms: List<ResponseForm>): String {
    val title = form.title.ifBlank { stringResource(R.string.untitled_form) }
    return if (forms.count { it.title == form.title } > 1) stringResource(R.string.dashboard_form_label, title, form.id) else title
}

@Composable
fun RequestSummaryCard(request: RequestSummary, onOpen: (RequestSummary) -> Unit) {
    val resources = LocalResources.current
    val submitted = request.submittedAt?.let {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(resources.configuration.locales[0]).withZone(ZoneId.systemDefault()).format(it)
    } ?: stringResource(R.string.request_detail_no_date)
    OutlinedCard(onClick = { onOpen(request) }, modifier = Modifier.fillMaxWidth().testTag("request-${request.key}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!request.isRead) Text(stringResource(R.string.dashboard_unread), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(request.formTitle.ifBlank { stringResource(R.string.untitled_form) }, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.request_detail_id, request.responseId))
            Text(stringResource(R.string.request_detail_submitted, submitted), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.request_detail_status, request.requestStatus?.let { stringResource(it.labelRes) }
                ?: request.statusLabel?.takeIf { it.isNotBlank() } ?: stringResource(R.string.request_detail_no_status)))
        }
    }
}

@Composable
fun ResponseLoadMessage(status: RequestListLoadStatus, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (status == RequestListLoadStatus.LOADING) CircularProgressIndicator()
        Text(stringResource(when (status) {
            RequestListLoadStatus.LOADING -> R.string.requests_loading
            RequestListLoadStatus.FORBIDDEN -> R.string.requests_forbidden
            RequestListLoadStatus.UNAVAILABLE -> R.string.requests_unavailable
            RequestListLoadStatus.EMPTY -> R.string.requests_empty
            else -> R.string.requests_error
        }))
        if (status == RequestListLoadStatus.ERROR || status == RequestListLoadStatus.FORBIDDEN) OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
