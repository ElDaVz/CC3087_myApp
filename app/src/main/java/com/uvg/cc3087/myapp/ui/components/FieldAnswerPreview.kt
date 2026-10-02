package com.uvg.cc3087.myapp.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.data.model.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FieldOptionsEditor(options: List<FieldOption>, onChange: (List<FieldOption>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            key(option.id) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = option.label,
                        onValueChange = { label -> onChange(options.map { if (it.id == option.id) it.copy(label = label) else it }) },
                        label = { Text("Opción") },
                        isError = option.label.isBlank(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onChange(options.filterNot { it.id == option.id }) }) { Text("Quitar") }
                }
            }
        }
        TextButton(onClick = { onChange(options + FieldOption(label = "")) }) { Text("+ Agregar opción") }
        if (options.size < 2) Text("Agrega al menos dos opciones", color = MaterialTheme.colorScheme.error)
    }
}

/** Disposable sample answers are deliberately separate from the form definition. */
@Composable
fun FieldAnswerPreview(field: FormFieldDraft) {
    var text by rememberSaveable(field.id) { mutableStateOf("") }
    var selected by rememberSaveable(field.id) { mutableStateOf<String?>(null) }
    var checkedIds by rememberSaveable(field.id, field.allowMultipleAnswers) { mutableStateOf(listOf<String>()) }
    var checked by rememberSaveable(field.id) { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    when (field.type) {
        FormFieldType.TEXT, FormFieldType.PARAGRAPH, FormFieldType.NUMBER, FormFieldType.EMAIL -> {
            val error = text.isNotEmpty() && when (field.type) {
                FormFieldType.NUMBER -> text.replace(',', '.').toBigDecimalOrNull() == null
                FormFieldType.EMAIL -> !Patterns.EMAIL_ADDRESS.matcher(text).matches()
                else -> false
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Respuesta de prueba") },
                singleLine = field.type != FormFieldType.PARAGRAPH,
                minLines = if (field.type == FormFieldType.PARAGRAPH) 4 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = when (field.type) {
                    FormFieldType.NUMBER -> KeyboardType.Decimal
                    FormFieldType.EMAIL -> KeyboardType.Email
                    else -> KeyboardType.Text
                }),
                isError = error,
                supportingText = { if (error) Text(if (field.type == FormFieldType.EMAIL) "Ingresa un correo válido" else "Ingresa un número válido") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        FormFieldType.MULTIPLE_CHOICE, FormFieldType.CHECKBOX_GROUP -> Column {
            field.options.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (field.type == FormFieldType.MULTIPLE_CHOICE) {
                        RadioButton(selected == option.id, onClick = { selected = option.id })
                    } else {
                        Checkbox(option.id in checkedIds, onCheckedChange = { value ->
                            checkedIds = when {
                                !value -> checkedIds - option.id
                                field.allowMultipleAnswers -> checkedIds + option.id
                                else -> listOf(option.id)
                            }
                        })
                    }
                    Text(option.label)
                }
            }
        }
        FormFieldType.CHECKBOX -> Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onCheckedChange = { checked = it })
            Text(field.title)
        }
        FormFieldType.DROPDOWN -> Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(field.options.firstOrNull { it.id == selected }?.label ?: "Selecciona una opción")
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                field.options.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { selected = option.id; expanded = false })
                }
            }
        }
        FormFieldType.DATE -> DateTimeAnswerPreview(field.id)
    }
}

@Composable
private fun DateTimeAnswerPreview(fieldId: String) {
    val context = LocalContext.current
    var date by rememberSaveable(fieldId) { mutableStateOf<String?>(null) }
    var time by rememberSaveable(fieldId) { mutableStateOf<String?>(null) }
    Column {
        OutlinedButton(onClick = {
            val initial = date?.let(LocalDate::parse) ?: LocalDate.now()
            DatePickerDialog(context, { _, year, month, day ->
                date = LocalDate.of(year, month + 1, day).toString()
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        }) { Text(date?.let { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) } ?: "Seleccionar fecha") }
        OutlinedButton(onClick = {
            val initial = time?.let(LocalTime::parse) ?: LocalTime.now()
            TimePickerDialog(context, { _, hour, minute -> time = LocalTime.of(hour, minute).toString() },
                initial.hour, initial.minute, DateFormat.is24HourFormat(context)).show()
        }) { Text(time ?: "Seleccionar hora") }
    }
}
