package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.domain.requests.ResponseFilter
import com.uvg.cc3087.myapp.domain.requests.ResponseForm
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus

@Composable
fun ResponseFilters(filter: ResponseFilter, forms: List<ResponseForm>, onForm: (String?) -> Unit, onPeriod: (ResponsePeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag("dashboard-form-filter")) {
                val selected = forms.find { it.id == filter.formId }
                val label = when {
                    filter.formId == null -> stringResource(R.string.dashboard_all_forms)
                    selected != null -> formLabel(selected, forms)
                    else -> stringResource(R.string.dashboard_missing_form)
                }
                Text(label, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
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
