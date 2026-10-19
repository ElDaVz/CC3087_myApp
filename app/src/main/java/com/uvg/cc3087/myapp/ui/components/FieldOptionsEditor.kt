package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.*

@Composable
fun FieldOptionsEditor(field: FormFieldDraft, onChange: (List<FieldOption>) -> Unit) {
    val options = field.options
    fun move(index: Int, direction: Int) {
        val target = index + direction
        if (target in options.indices) {
            onChange(options.toMutableList().apply { add(target, removeAt(index)) })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, option ->
            key(option.id) {
                Column {
                    OutlinedTextField(
                        value = option.label,
                        onValueChange = { label -> onChange(options.map { if (it.id == option.id) it.copy(label = label) else it }) },
                        placeholder = { Text(stringResource(R.string.option_position, index + 1)) },
                        isError = option.label.isBlank() || options.any {
                            it.id != option.id && it.label.trim().equals(option.label.trim(), ignoreCase = true)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.align(Alignment.End)) {
                        IconButton(onClick = { move(index, -1) }, enabled = index > 0) {
                            Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.move_option_up, index + 1))
                        }
                        IconButton(onClick = { move(index, 1) }, enabled = index < options.lastIndex) {
                            Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.move_option_down, index + 1))
                        }
                        IconButton(onClick = { onChange(options.filterNot { it.id == option.id }) }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.remove_option, index + 1))
                        }
                    }
                }
            }
        }
        TextButton(onClick = { onChange(options + FieldOption(label = "")) }) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_option))
        }
        field.optionError()?.let { Text(fieldErrorText(it, field), color = MaterialTheme.colorScheme.error) }
    }
}
