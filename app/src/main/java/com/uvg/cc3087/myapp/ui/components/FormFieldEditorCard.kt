package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.*

@Composable
fun FormFieldEditorCard(
    field: FormFieldDraft,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onFieldChange: (FormFieldDraft) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shouldFocusTitle: Boolean = false,
    onTitleFocusRequested: () -> Unit = {}
) {
    val titleFocusRequester = remember(field.id) { FocusRequester() }
    val errors = field.configurationErrors()
    LaunchedEffect(shouldFocusTitle) {
        if (shouldFocusTitle) {
            titleFocusRequester.requestFocus()
            onTitleFocusRequested()
        }
    }
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.field_position, position + 1), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(field.type.labelRes), style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.move_field_up, position + 1), Modifier.size(20.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.move_field_down, position + 1), Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.delete_field))
                }
            }
            OutlinedTextField(
                value = field.title,
                onValueChange = { onFieldChange(field.copy(title = it)) },
                placeholder = { Text(stringResource(R.string.question_placeholder)) },
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester).testTag("question-${field.id}"),
                minLines = 1,
                maxLines = 4,
                isError = FieldError.MISSING_QUESTION in errors
            )
            OutlinedTextField(
                value = field.helpText,
                onValueChange = { onFieldChange(field.copy(helpText = it)) },
                label = { Text(stringResource(R.string.help_text_label)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            if (field.type in listOf(FormFieldType.TEXT, FormFieldType.PARAGRAPH, FormFieldType.EMAIL, FormFieldType.NUMBER)) {
                OutlinedTextField(
                    value = field.placeholder,
                    onValueChange = { onFieldChange(field.copy(placeholder = it)) },
                    label = { Text(stringResource(R.string.answer_placeholder_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            if (field.type.hasOptions) {
                Text(stringResource(R.string.allowed_answers), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !field.isMultipleSelection,
                        onClick = { onFieldChange(field.withSelectionMode(false)) },
                        label = { Text(stringResource(R.string.answer_single)) }
                    )
                    FilterChip(
                        selected = field.isMultipleSelection,
                        onClick = { onFieldChange(field.withSelectionMode(true)) },
                        label = { Text(stringResource(R.string.answer_multiple)) }
                    )
                }
                FieldOptionsEditor(field, onChange = { onFieldChange(field.copy(options = it)) })
            }
            HorizontalDivider()
            Text(stringResource(R.string.field_settings), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            FieldSettingSwitch(stringResource(R.string.required), field.required) {
                onFieldChange(field.copy(required = it))
            }
            if (field.type == FormFieldType.DATE) {
                FieldSettingSwitch(stringResource(R.string.include_time), field.includeTime) {
                    onFieldChange(field.copy(includeTime = it))
                }
                FieldSettingSwitch(stringResource(R.string.disallow_past_dates), field.disallowPastDates) {
                    onFieldChange(field.copy(disallowPastDates = it))
                }
            }
            if (field.type == FormFieldType.NUMBER) NumberFieldSettings(field, onFieldChange)
        }
    }
}

@Composable
private fun FieldSettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // el texto y el switch responden como un solo control accesible
    androidx.compose.material3.ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

@Composable
private fun NumberFieldSettings(field: FormFieldDraft, onChange: (FormFieldDraft) -> Unit) {
    val locale = currentFormLocale()
    val errors = field.configurationErrors()
    Text(stringResource(R.string.number_format), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!field.allowDecimals, onClick = { onChange(field.copy(allowDecimals = false)) },
            label = { Text(stringResource(R.string.number_integer)) })
        FilterChip(field.allowDecimals, onClick = { onChange(field.copy(allowDecimals = true)) },
            label = { Text(stringResource(R.string.number_decimal)) })
    }
    OutlinedTextField(
        value = localizedNumber(field.minimum, locale),
        onValueChange = { onChange(field.copy(minimum = canonicalNumber(it, locale))) },
        label = { Text(stringResource(R.string.minimum_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = FieldError.INVALID_MINIMUM in errors || FieldError.INVALID_RANGE in errors,
        supportingText = { if (FieldError.INVALID_MINIMUM in errors) Text(fieldErrorText(FieldError.INVALID_MINIMUM, field)) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = localizedNumber(field.maximum, locale),
        onValueChange = { onChange(field.copy(maximum = canonicalNumber(it, locale))) },
        label = { Text(stringResource(R.string.maximum_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = FieldError.INVALID_MAXIMUM in errors || FieldError.INVALID_RANGE in errors,
        supportingText = {
            if (FieldError.INVALID_MAXIMUM in errors) Text(fieldErrorText(FieldError.INVALID_MAXIMUM, field))
            else if (FieldError.INVALID_RANGE in errors) Text(fieldErrorText(FieldError.INVALID_RANGE, field))
        },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = field.unit,
        onValueChange = { onChange(field.copy(unit = it)) },
        label = { Text(stringResource(R.string.unit_label)) },
        placeholder = { Text(stringResource(R.string.unit_example)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
