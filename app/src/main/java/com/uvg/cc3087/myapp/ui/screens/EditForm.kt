package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.components.AddFieldBottomSheet
import com.uvg.cc3087.myapp.ui.components.FormFieldEditorCard
import com.uvg.cc3087.myapp.ui.components.DraftLoadStatus
import com.uvg.cc3087.myapp.ui.components.DraftSaveNotice
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
    modifier: Modifier = Modifier,
    onRetrySave: () -> Unit = {},
    onRetryLoad: () -> Unit = {},
    onSaveForm: (() -> Unit)? = null,
    configurationErrors: () -> Map<String, Set<com.uvg.cc3087.myapp.data.model.FieldError>> = { emptyMap() },
    validatePreviewAnswers: (Map<String, com.uvg.cc3087.myapp.data.model.FormAnswer>, Boolean, java.util.Locale) -> Map<String, com.uvg.cc3087.myapp.data.model.FieldError> = { _, _, _ -> emptyMap() },
    canAdvanceToSaveShare: () -> Boolean = { false }
) {
    val resources = LocalResources.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // estos estados solo controlan la presentación, el borrador sigue en el viewmodel
    var showFieldCatalog by rememberSaveable { mutableStateOf(false) }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var pendingInsertion by remember { mutableStateOf(false) }
    var pendingScrollId by remember { mutableStateOf<String?>(null) }
    var focusedFieldId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.fields, showFieldCatalog, pendingScrollId) {
        if (showFieldCatalog) return@LaunchedEffect
        val fieldId = if (pendingInsertion) uiState.fields.lastOrNull()?.id else pendingScrollId
        val index = uiState.fields.indexOfFirst { it.id == fieldId }
        if (index >= 0) {
            listState.animateScrollToItem(index + 1)
            if (pendingInsertion) focusedFieldId = fieldId
        }
        pendingInsertion = false
        pendingScrollId = null
    }

    fun moveField(fieldId: String, direction: FieldMoveDirection) {
        // enviamos la intención por id y dejamos el nuevo orden en manos del viewmodel
        onAction(EditFormAction.MoveField(fieldId, direction))
        pendingScrollId = fieldId
    }

    val canEdit = !uiState.isLoading && uiState.loadError == null
    if (showPreview && canEdit) {
        FormPreviewScreen(
            title = uiState.title,
            fields = uiState.fields,
            onClose = { showPreview = false },
            onSaveForm = onSaveForm,
            saveStatus = uiState.saveStatus,
            onRetrySave = onRetrySave,
            configurationErrors = configurationErrors(),
            validateAnswers = validatePreviewAnswers
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.edit_form_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                stringResource(R.string.go_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(enabled = canEdit, onClick = {
                            focusManager.clearFocus()
                            showPreview = true
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = stringResource(R.string.preview)
                            )
                        }
                    }
                )
                if (canEdit) {
                    DraftSaveNotice(
                        uiState.saveStatus,
                        onRetrySave,
                        Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canEdit) {
                ExtendedFloatingActionButton(
                    onClick = { showFieldCatalog = true },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_field)) }
                )
            }
        }
    ) { innerPadding ->
        if (!canEdit) {
            DraftLoadStatus(
                error = uiState.loadError,
                onRetry = onRetryLoad,
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            )
        } else LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "form-details") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.form_details),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { onAction(EditFormAction.ChangeTitle(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.form_title)) },
                        singleLine = true,
                        isError = uiState.hasTitleError,
                        supportingText = {
                            if (uiState.hasTitleError) Text(stringResource(R.string.error_form_title))
                        }
                    )
                }
            }
            if (uiState.fields.isEmpty()) {
                item(key = "empty-fields") {
                    Text(
                        stringResource(R.string.empty_fields),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(uiState.fields, key = { _, field -> field.id }) { index, field ->
                FormFieldEditorCard(
                    field = field,
                    position = index,
                    canMoveUp = index > 0,
                    canMoveDown = index < uiState.fields.lastIndex,
                    onFieldChange = { onAction(EditFormAction.UpdateField(it)) },
                    onMoveUp = { moveField(field.id, FieldMoveDirection.UP) },
                    onMoveDown = { moveField(field.id, FieldMoveDirection.DOWN) },
                    onDelete = { onAction(EditFormAction.DeleteField(field.id)) },
                    shouldFocusTitle = field.id == focusedFieldId,
                    onTitleFocusRequested = { focusedFieldId = null }
                )
            }
            item(key = "validate-draft") {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val message = resources.getString(onValidate().messageResId)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                ) {
                    Text(stringResource(R.string.editor_validate))
                }
            }
        }
    }

    if (showFieldCatalog && canEdit) {
        AddFieldBottomSheet(
            onDismissRequest = { showFieldCatalog = false },
            onFieldSelected = { type ->
                // el viewmodel crea el id y la ui espera el campo nuevo para enfocarlo
                pendingInsertion = true
                onAction(EditFormAction.AddField(type))
                showFieldCatalog = false
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditFormPreview() {
    MyappTheme(dynamicColor = false) {
        EditForm(
            uiState = EditFormUiState(
                title = stringResource(R.string.template_job_application),
                fields = FormEditorSampleData.templateFields(LocalResources.current)
            ),
            onAction = {},
            onValidate = { FormValidationResult.READY },
            onBackClick = {}
        )
    }
}
