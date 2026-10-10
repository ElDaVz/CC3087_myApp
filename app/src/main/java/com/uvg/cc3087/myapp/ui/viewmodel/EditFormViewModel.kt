package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.domain.validation.FormDraftValidator
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.state.EditFormAction
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.state.FieldMoveDirection
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditFormViewModel(
    private val templateRepository: TemplateRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<EditFormUiState> = mutableUiState.asStateFlow()

    fun startDraft(title: String, templateType: TemplateType?) {
        // la ui pasa la selección y aquí obtenemos los datos para iniciar el editor
        val fields = templateType?.let(templateRepository::getInitialFields).orEmpty()
        // entrar desde una plantilla crea un borrador nuevo, aunque el título sea el mismo
        updateState(EditFormUiState(title = title, fields = fields.toList()))
    }

    fun onAction(action: EditFormAction) {
        val current = mutableUiState.value
        val updated = when (action) {
            is EditFormAction.ChangeTitle -> current.copy(title = action.title)
            is EditFormAction.AddField -> current.copy(
                fields = current.fields + FormFieldDraft(
                    id = "field-${UUID.randomUUID()}",
                    type = action.type,
                    // conservamos el texto recibido, sin buscar recursos ni decidir el idioma
                    title = action.initialTitle
                )
            )
            is EditFormAction.ChangeFieldTitle -> current.copy(
                fields = current.fields.map { field ->
                    if (field.id == action.fieldId) field.copy(title = action.title) else field
                }
            )
            is EditFormAction.SetFieldRequired -> current.copy(
                fields = current.fields.map { field ->
                    if (field.id == action.fieldId) field.copy(required = action.required) else field
                }
            )
            is EditFormAction.MoveField -> current.copy(
                fields = moveField(current.fields, action.fieldId, action.direction)
            )
            is EditFormAction.DeleteField -> current.copy(
                fields = current.fields.filterNot { it.id == action.fieldId }
            )
        }
        if (updated != current) updateState(updated)
    }

    fun validate(): FormValidationResult {
        val current = mutableUiState.value
        return FormDraftValidator.validate(current.title, current.fields)
    }

    private fun moveField(
        fields: List<FormFieldDraft>,
        fieldId: String,
        direction: FieldMoveDirection
    ): List<FormFieldDraft> {
        // usamos el id para no mover otro campo si cambió la posición de la tarjeta
        val fromIndex = fields.indexOfFirst { it.id == fieldId }
        if (fromIndex == -1) return fields
        val targetIndex = fromIndex + direction.offset
        if (targetIndex !in fields.indices) return fields

        return fields.toMutableList().apply {
            val movedField = removeAt(fromIndex)
            add(targetIndex, movedField)
        }
    }

    private fun updateState(updated: EditFormUiState) {
        savedStateHandle[TITLE_KEY] = updated.title
        // guardamos valores simples, no instancias de modelos ni recursos de android
        savedStateHandle[FIELDS_KEY] = ArrayList(updated.fields.flatMap { field ->
            listOf(field.id, field.type.name, field.title, field.required.toString())
        })
        mutableUiState.value = updated
    }

    private fun restoreState(): EditFormUiState {
        val title = savedStateHandle.get<String>(TITLE_KEY).orEmpty()
        val values = savedStateHandle.get<ArrayList<String>>(FIELDS_KEY).orEmpty()
        if (values.size % FIELD_VALUE_COUNT != 0) return EditFormUiState(title = title)

        val fields = values.chunked(FIELD_VALUE_COUNT).map { fieldValues ->
            val type = FormFieldType.entries.firstOrNull { it.name == fieldValues[1] }
                ?: return EditFormUiState(title = title)
            val required = fieldValues[3].toBooleanStrictOrNull()
                ?: return EditFormUiState(title = title)
            FormFieldDraft(
                id = fieldValues[0],
                type = type,
                title = fieldValues[2],
                required = required
            )
        }
        return EditFormUiState(title = title, fields = fields)
    }

    companion object {
        private const val TITLE_KEY = "editor_title"
        private const val FIELDS_KEY = "editor_fields"
        private const val FIELD_VALUE_COUNT = 4

        fun factory(templateRepository: TemplateRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditFormViewModel(templateRepository, createSavedStateHandle()) }
        }
    }
}
