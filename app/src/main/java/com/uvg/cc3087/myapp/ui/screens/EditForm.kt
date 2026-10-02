package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    var fields by remember(initialTitle) {
        mutableStateOf(initialFields)
    }
    var nextFieldNumber by rememberSaveable(initialTitle) {
        mutableStateOf(initialFields.size + 1)
    }
    var showFieldCatalog by rememberSaveable { mutableStateOf(false) }
    var focusedFieldId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun addField(type: FormFieldType) {
        val fieldId = "field-$nextFieldNumber"
        fields = fields + FormFieldDraft(
            id = fieldId,
            type = type,
            title = type.defaultTitle
        )
        nextFieldNumber += 1
        focusedFieldId = fieldId
    }

    LaunchedEffect(focusedFieldId, fields.size) {
        val fieldId = focusedFieldId ?: return@LaunchedEffect
        val fieldIndex = fields.indexOfFirst { field -> field.id == fieldId }
        if (fieldIndex >= 0) {
            listState.animateScrollToItem(fieldIndex + 1)
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
    }

    fun validateForm() {
        when {
            formTitle.isBlank() -> showMessage("Escribe el título del formulario")
            fields.isEmpty() -> showMessage("Agrega al menos un campo")
            fields.any { it.title.isBlank() } -> showMessage("Completa el nombre de todos los campos")
            else -> showMessage("Formulario listo para continuar :D")
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Editar formulario") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver a plantillas"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::validateForm) {
                        Text("Validar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showFieldCatalog = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Agregar campo") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "form-details") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Información del formulario",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título") },
                        singleLine = true,
                        isError = formTitle.isBlank()
                    )
                }
            }

            if (fields.isEmpty()) {
                item(key = "empty-fields") {
                    Text(
                        text = "Todavía no hay campos. Agrega uno para comenzar",
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
                    onTitleChange = { newTitle ->
                        updateField(field.copy(title = newTitle))
                    },
                    onRequiredChange = { required ->
                        updateField(field.copy(required = required))
                    },
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
            initialTitle = "Solicitud de empleo",
            initialFields = FormEditorSampleData.templateFields,
            onBackClick = {}
        )
    }
}
