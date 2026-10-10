package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.components.FormFieldEditorCard
import com.uvg.cc3087.myapp.ui.resources.addActionResId
import com.uvg.cc3087.myapp.ui.resources.defaultTitleResId
import com.uvg.cc3087.myapp.ui.resources.messageResId
import com.uvg.cc3087.myapp.ui.state.EditFormAction
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.state.FieldMoveDirection
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditForm(
    uiState: EditFormUiState,
    onAction: (EditFormAction) -> Unit,
    onValidate: () -> FormValidationResult,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val fieldIdsWithTitleErrors = uiState.fieldIdsWithTitleErrors

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun validateForm() {
        // el viewmodel valida y la ui elige cómo mostrar el resultado
        showMessage(context.getString(onValidate().messageResId))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_heading)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back_to_templates)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::validateForm) {
                        Text(stringResource(R.string.editor_validate))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "form-details") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.editor_form_information),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { onAction(EditFormAction.ChangeTitle(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.editor_form_title)) },
                        singleLine = true,
                        isError = uiState.hasTitleError
                    )
                }
            }

            item(key = "add-field-controls") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.editor_add_field),
                        style = MaterialTheme.typography.titleMedium
                    )

                    FormFieldType.entries.forEach { type ->
                        val initialTitle = stringResource(type.defaultTitleResId)
                        OutlinedButton(
                            onClick = { onAction(EditFormAction.AddField(type, initialTitle)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(type.addActionResId))
                        }
                    }
                }
            }

            if (uiState.fields.isEmpty()) {
                item(key = "empty-fields") {
                    Text(
                        text = stringResource(R.string.editor_empty_fields),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(
                items = uiState.fields,
                key = { _, field -> field.id }
            ) { index, field ->
                FormFieldEditorCard(
                    field = field,
                    position = index,
                    canMoveUp = index > 0,
                    canMoveDown = index < uiState.fields.lastIndex,
                    isTitleError = field.id in fieldIdsWithTitleErrors,
                    onTitleChange = { newTitle ->
                        onAction(EditFormAction.ChangeFieldTitle(field.id, newTitle))
                    },
                    onRequiredChange = { required ->
                        onAction(EditFormAction.SetFieldRequired(field.id, required))
                    },
                    onMoveUp = { onAction(EditFormAction.MoveField(field.id, FieldMoveDirection.UP)) },
                    onMoveDown = {
                        onAction(EditFormAction.MoveField(field.id, FieldMoveDirection.DOWN))
                    },
                    onDelete = { onAction(EditFormAction.DeleteField(field.id)) }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditFormPreview() {
    MyappTheme(dynamicColor = false) {
        EditForm(
            uiState = EditFormUiState(
                title = "Solicitud de empleo",
                fields = FormEditorSampleData.templateFields
            ),
            onAction = {},
            onValidate = { FormValidationResult.READY },
            onBackClick = {}
        )
    }
}
