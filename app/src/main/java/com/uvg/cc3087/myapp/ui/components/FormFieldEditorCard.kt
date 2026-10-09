package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.uvg.cc3087.myapp.data.model.FormFieldType
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
import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.hasOptions

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
    onMultipleAnswersChange: (Boolean) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onOptionsChange: (List<FieldOption>) -> Unit = {},
    shouldFocusTitle: Boolean = false,
    onTitleFocusRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val titleFocusRequester = remember(field.id) { FocusRequester() }
    val dragStart by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val dragCancel by rememberUpdatedState(onDragCancel)

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
                Icon(
                    Icons.Outlined.DragIndicator,
                    contentDescription = "Arrastra para reordenar",
                    modifier = Modifier.size(48.dp)
                        .semantics {
                            customActions = buildList {
                                if (canMoveUp) add(CustomAccessibilityAction("Mover arriba") { onMoveUp(); true })
                                if (canMoveDown) add(CustomAccessibilityAction("Mover abajo") { onMoveDown(); true })
                            }
                        }
                        .pointerInput(field.id) {
                            detectDragGestures(
                                onDragStart = { dragStart() },
                                onDragEnd = { dragEnd() },
                                onDragCancel = { dragCancel() },
                                onDrag = { change, amount -> change.consume(); drag(amount.y) }
                            )
                        }
                )
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

            if (field.type.hasOptions) {
                FieldOptionsEditor(field.options, onOptionsChange)
            }
            if (field.type == FormFieldType.CHECKBOX_GROUP) {
                Text("Respuestas permitidas", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !field.allowMultipleAnswers,
                        onClick = { onMultipleAnswersChange(false) },
                        label = { Text("Una sola respuesta") }
                    )
                    FilterChip(
                        selected = field.allowMultipleAnswers,
                        onClick = { onMultipleAnswersChange(true) },
                        label = { Text("Varias respuestas") }
                    )
                }
            }
            Text("Vista previa interactiva", style = MaterialTheme.typography.labelLarge)
            FieldAnswerPreview(field)

            HorizontalDivider()
            Text("Configuración del campo", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Obligatorio")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = field.required, onCheckedChange = onRequiredChange)
            }
        }
    }
}
