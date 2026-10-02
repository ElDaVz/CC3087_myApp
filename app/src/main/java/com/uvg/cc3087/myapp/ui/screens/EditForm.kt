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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
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
import com.uvg.cc3087.myapp.data.model.hasValidOptions
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
    var showFieldCatalog by rememberSaveable { mutableStateOf(false) }
    var focusedFieldId by remember { mutableStateOf<String?>(null) }
    var pendingFieldId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragY by remember { mutableStateOf(0f) }
    val handleOffset = with(LocalDensity.current) { 40.dp.toPx() }
    val edgeSize = with(LocalDensity.current) { 72.dp.toPx() }

    LaunchedEffect(draggedId) {
        if (draggedId == null) return@LaunchedEffect
        while (true) {
            val layout = listState.layoutInfo
            val scroll = when {
                dragY < layout.viewportStartOffset + edgeSize -> -18f
                dragY > layout.viewportEndOffset - edgeSize -> 18f
                else -> 0f
            }
            if (scroll != 0f) listState.scrollBy(scroll)
            delay(16)
        }
    }

    fun finishDrag() {
        val id = draggedId ?: return
        val target = listState.layoutInfo.visibleItemsInfo
            .filter { item -> fields.any { it.id == item.key } }
            .minByOrNull { item ->
                when {
                    dragY < item.offset -> item.offset - dragY
                    dragY > item.offset + item.size -> dragY - item.offset - item.size
                    else -> 0f
                }
            }
        val from = fields.indexOfFirst { it.id == id }
        val to = fields.indexOfFirst { it.id == target?.key }
        if (from >= 0 && to >= 0 && from != to) {
            fields = fields.toMutableList().apply { add(to, removeAt(from)) }
        }
        draggedId = null
    }

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun addField(type: FormFieldType) {
        val fieldId = java.util.UUID.randomUUID().toString()
        fields = fields + FormFieldDraft(
            id = fieldId,
            type = type,
            title = type.defaultTitle
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
    }

    fun validateForm() {
        when {
            formTitle.isBlank() -> showMessage("Escribe el título del formulario")
            fields.isEmpty() -> showMessage("Agrega al menos un campo")
            fields.any { it.title.isBlank() } -> showMessage("Completa el nombre de todos los campos")
            fields.any { !it.hasValidOptions() } ->
                showMessage("Agrega al menos dos opciones con nombre en cada campo de selección")
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
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
                    modifier = Modifier.zIndex(if (draggedId == field.id) 1f else 0f)
                        .graphicsLayer {
                            if (draggedId == field.id) {
                                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == field.id }
                                translationY = dragY - (item?.offset ?: 0) - handleOffset
                                alpha = 0.85f
                            }
                        },
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
                    onOptionsChange = { updateField(field.copy(options = it)) },
                    onMultipleAnswersChange = { updateField(field.copy(allowMultipleAnswers = it)) },
                    onDragStart = {
                        draggedId = field.id
                        dragY = (listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == field.id }?.offset ?: 0) + handleOffset
                    },
                    onDrag = { dragY += it },
                    onDragEnd = ::finishDrag,
                    onDragCancel = { draggedId = null },
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
