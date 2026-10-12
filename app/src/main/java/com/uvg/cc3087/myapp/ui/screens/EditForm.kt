package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.ui.components.FormFieldEditorCard
import com.uvg.cc3087.myapp.ui.components.AddFieldBottomSheet
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditForm(
    initialTitle: String,
    initialFields: List<FormFieldDraft>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var formTitle by rememberSaveable(initialTitle) {
        mutableStateOf(initialTitle)
    }
    var fields by rememberSaveable(initialTitle, stateSaver = listSaver<List<FormFieldDraft>, FormFieldDraft>(
        save = { it }, restore = { it }
    )) {
        mutableStateOf(initialFields)
    }
    var showFieldCatalog by rememberSaveable { mutableStateOf(false) }
    var focusedFieldId by remember { mutableStateOf<String?>(null) }
    var pendingFieldId by remember { mutableStateOf<String?>(null) }

    var showPreview by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    fun addField(type: FormFieldType) {
        val fieldId = java.util.UUID.randomUUID().toString()
        fields = fields + FormFieldDraft(
            id = fieldId,
            type = type,
            title = "",
            includeTime = false,
            allowDecimals = false
        )
        pendingFieldId = fieldId
    }

    LaunchedEffect(pendingFieldId, showFieldCatalog) {
        if (showFieldCatalog) return@LaunchedEffect
        val fieldId = pendingFieldId ?: return@LaunchedEffect
        val fieldIndex = fields.indexOfFirst { field -> field.id == fieldId }
        if (fieldIndex >= 0) {
            listState.animateScrollToItem(fieldIndex + 1)
            focusedFieldId = fieldId
            pendingFieldId = null
        }
    }

    fun updateField(updatedField: FormFieldDraft) {
        fields = fields.map { field ->
            if (field.id == updatedField.id) updatedField else field
        }
    }

    fun moveField(fromIndex: Int, direction: Int) {
        val targetIndex = fromIndex + direction
        if (targetIndex !in fields.indices) return

        val reorderedFields = fields.toMutableList()
        val movedField = reorderedFields.removeAt(fromIndex)
        reorderedFields.add(targetIndex, movedField)
        fields = reorderedFields
        coroutineScope.launch { listState.animateScrollToItem(targetIndex + 1) }
    }

    if (showPreview) {
        FormPreviewScreen(
            title = formTitle,
            fields = fields,
            onClose = { showPreview = false }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_form_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_templates)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { focusManager.clearFocus(); showPreview = true }) {
                        Text(stringResource(R.string.preview))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showFieldCatalog = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_field)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "form-details") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.form_details),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.form_title)) },
                        singleLine = true,
                        isError = formTitle.isBlank(),
                        supportingText = { if (formTitle.isBlank()) Text(stringResource(R.string.error_form_title)) }
                    )
                }
            }

            if (fields.isEmpty()) {
                item(key = "empty-fields") {
                    Text(
                        text = stringResource(R.string.empty_fields),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(
                items = fields,
                key = { _, field -> field.id }
            ) { index, field ->
                FormFieldEditorCard(
                    field = field,
                    position = index,
                    canMoveUp = index > 0,
                    canMoveDown = index < fields.lastIndex,
                    onFieldChange = ::updateField,
                    onMoveUp = { moveField(index, -1) },
                    onMoveDown = { moveField(index, 1) },
                    onDelete = {
                        fields = fields.filterNot { it.id == field.id }
                    },
                    shouldFocusTitle = field.id == focusedFieldId,
                    onTitleFocusRequested = {
                        focusedFieldId = null
                    }
                )
            }
        }
    }

    if (showFieldCatalog) {
        AddFieldBottomSheet(
            onDismissRequest = { showFieldCatalog = false },
            onFieldSelected = { type ->
                addField(type)
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
            initialTitle = stringResource(R.string.template_job_application),
            initialFields = FormEditorSampleData.templateFields(LocalResources.current),
            onBackClick = {}
        )
    }
}
