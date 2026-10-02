package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.data.model.FormFieldDraft

@Composable
fun FormFieldEditorCard(
    field: FormFieldDraft,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTitleChange: (String) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    shouldFocusTitle: Boolean = false,
    onTitleFocusRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val titleFocusRequester = remember(field.id) { FocusRequester() }

    LaunchedEffect(shouldFocusTitle) {
        if (shouldFocusTitle) {
            titleFocusRequester.requestFocus()
            onTitleFocusRequested()
        }
    }

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Campo ${position + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = field.type.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar campo"
                    )
                }
            }

            OutlinedTextField(
                value = field.title,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                label = { Text("Nombre del campo") },
                singleLine = true,
                isError = field.title.isBlank(),
                supportingText = {
                    if (field.title.isBlank()) {
                        Text("El nombre no puede quedar vacío")
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = field.required,
                    onCheckedChange = onRequiredChange
                )
                Text("Obligatorio")
                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Mover campo hacia arriba"
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Mover campo hacia abajo"
                    )
                }
            }
        }
    }
}
