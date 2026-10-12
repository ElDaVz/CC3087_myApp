package com.uvg.cc3087.myapp.ui.components

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FormAnswerInput(
    field: FormFieldDraft,
    answer: FormAnswer,
    onChange: (FormAnswer) -> Unit,
    isError: Boolean,
    questionLabel: String = field.title
) {
    when (field.type) {
        FormFieldType.TEXT, FormFieldType.PARAGRAPH, FormFieldType.NUMBER, FormFieldType.EMAIL -> {
            OutlinedTextField(
                value = answer.text,
                onValueChange = { onChange(answer.copy(text = it)) },
                label = { Text(stringResource(R.string.your_answer)) },
                placeholder = { if (field.placeholder.isNotBlank()) Text(field.placeholder) },
                suffix = if (field.type == FormFieldType.NUMBER && field.unit.isNotBlank()) {{ Text(field.unit) }} else null,
                singleLine = field.type != FormFieldType.PARAGRAPH,
                minLines = if (field.type == FormFieldType.PARAGRAPH) 4 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = when (field.type) {
                    FormFieldType.NUMBER -> if (field.allowDecimals) KeyboardType.Decimal else KeyboardType.Number
                    FormFieldType.EMAIL -> KeyboardType.Email
                    else -> KeyboardType.Text
                }),
                isError = isError,
                modifier = Modifier.fillMaxWidth().testTag("answer-${field.id}")
            )
        }
        FormFieldType.MULTIPLE_CHOICE, FormFieldType.CHECKBOX_GROUP -> {
            Column(Modifier.then(if (field.isMultipleSelection) Modifier else Modifier.selectableGroup())) {
                field.options.forEach { option ->
                    ChoiceRow(option, option.id in answer.selectedIds, field.isMultipleSelection) {
                        onChange(answer.select(option.id, field.isMultipleSelection))
                    }
                }
                if (answer.selectedIds.isNotEmpty() && !field.required) {
                    TextButton(onClick = { onChange(answer.copy(selectedIds = emptySet())) }) {
                        Text(stringResource(R.string.clear_answer))
                    }
                }
            }
        }
        FormFieldType.CHECKBOX -> Row(
            modifier = Modifier.fillMaxWidth().toggleable(
                answer.checked, role = Role.Checkbox,
                onValueChange = { onChange(answer.copy(checked = it)) }
            ).padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(answer.checked, onCheckedChange = null)
            Text(questionLabel, style = MaterialTheme.typography.titleMedium)
        }
        FormFieldType.DROPDOWN -> DropdownAnswerInput(field, answer, onChange, isError)
        FormFieldType.DATE -> DateTimeInput(field, answer, onChange, isError)
    }
}

private fun FormAnswer.select(id: String, multiple: Boolean): FormAnswer = copy(
    selectedIds = if (!multiple) setOf(id) else if (id in selectedIds) selectedIds - id else selectedIds + id
)

@Composable
private fun ChoiceRow(option: FieldOption, selected: Boolean, multiple: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().then(
            if (multiple) Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onSelect() })
            else Modifier.selectable(selected, role = Role.RadioButton, onClick = onSelect)
        ).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (multiple) Checkbox(selected, onCheckedChange = null) else RadioButton(selected, onClick = null)
        Text(option.label)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DropdownAnswerInput(field: FormFieldDraft, answer: FormAnswer, onChange: (FormAnswer) -> Unit, isError: Boolean) {
    var expanded by rememberSaveable(field.id) { mutableStateOf(false) }
    val selected = field.options.filter { it.id in answer.selectedIds }
    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag("dropdown-${field.id}"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor =
                if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        ) {
            Text(
                if (selected.isEmpty()) stringResource(if (field.isMultipleSelection) R.string.select_several else R.string.select_one)
                else if (field.isMultipleSelection) pluralStringResource(R.plurals.selected_options_count, selected.size, selected.size)
                else selected.first().label,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        if (field.isMultipleSelection) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.forEach { option ->
                    InputChip(
                        selected = true,
                        onClick = { onChange(answer.copy(selectedIds = answer.selectedIds - option.id)) },
                        label = { Text(option.label) },
                        trailingIcon = { Icon(Icons.Outlined.Close, stringResource(R.string.remove_selection, option.label), Modifier.size(18.dp)) }
                    )
                }
            }
        }
        if (selected.isNotEmpty() && !field.required) {
            TextButton(onClick = { onChange(answer.copy(selectedIds = emptySet())) }) { Text(stringResource(R.string.clear_answer)) }
        }
    }
    if (expanded) {
        var query by rememberSaveable { mutableStateOf("") }
        val visible = field.options.filter { it.label.contains(query, ignoreCase = true) }
        ModalBottomSheet(onDismissRequest = { expanded = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(field.title, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(stringResource(R.string.search_options)) }, modifier = Modifier.fillMaxWidth())
                if (visible.isEmpty()) Text(stringResource(R.string.no_matching_options))
                LazyColumn(Modifier.weight(1f, fill = false).then(if (field.isMultipleSelection) Modifier else Modifier.selectableGroup())) {
                    items(visible, key = { it.id }) { option ->
                        ChoiceRow(option, option.id in answer.selectedIds, field.isMultipleSelection) {
                            onChange(answer.select(option.id, field.isMultipleSelection))
                            if (!field.isMultipleSelection) expanded = false
                        }
                    }
                }
                TextButton(onClick = { expanded = false }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}

@Composable
private fun DateTimeInput(field: FormFieldDraft, answer: FormAnswer, onChange: (FormAnswer) -> Unit, isError: Boolean) {
    val context = LocalContext.current
    val locale = currentFormLocale()
    val latestAnswer by rememberUpdatedState(answer)
    val latestOnChange by rememberUpdatedState(onChange)
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    DisposableEffect(context) { onDispose { dialog?.dismiss() } }
    val date = runCatching { LocalDate.parse(answer.date) }.getOrNull()
    val time = runCatching { LocalTime.parse(answer.time) }.getOrNull()
    val colors = ButtonDefaults.outlinedButtonColors(contentColor =
        if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    Column {
        OutlinedButton(onClick = {
            val today = LocalDate.now()
            val initial = (date ?: today).let { if (field.disallowPastDates && it < today) today else it }
            dialog = DatePickerDialog(context, { _, year, month, day ->
                latestOnChange(latestAnswer.copy(date = LocalDate.of(year, month + 1, day).toString()))
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
                if (field.disallowPastDates) datePicker.minDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                show()
            }
        }, colors = colors, modifier = Modifier.fillMaxWidth()) {
            Text(if (date == null) stringResource(R.string.select_date) else stringResource(
                R.string.date_value, date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))))
        }
        if (field.includeTime) {
            OutlinedButton(onClick = {
                val initial = time ?: LocalTime.now()
                dialog = TimePickerDialog(context, { _, hour, minute ->
                    latestOnChange(latestAnswer.copy(time = LocalTime.of(hour, minute).toString()))
                }, initial.hour, initial.minute, DateFormat.is24HourFormat(context)).apply { show() }
            }, colors = colors, modifier = Modifier.fillMaxWidth()) {
                val formatter = DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
                Text(if (time == null) stringResource(R.string.select_time) else stringResource(R.string.time_value, time.format(formatter)))
            }
        }
        if (answer.date.isNotBlank() || answer.time.isNotBlank()) {
            TextButton(onClick = { onChange(answer.copy(date = "", time = "")) }) { Text(stringResource(R.string.clear_answer)) }
        }
    }
}
