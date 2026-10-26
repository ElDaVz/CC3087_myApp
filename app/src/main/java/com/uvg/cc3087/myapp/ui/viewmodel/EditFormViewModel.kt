package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormAnswer
import com.uvg.cc3087.myapp.data.model.FieldError
import com.uvg.cc3087.myapp.data.model.answerError
import com.uvg.cc3087.myapp.data.model.configurationErrors
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.domain.validation.FormDraftValidator
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.state.EditFormAction
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.state.FieldMoveDirection
import com.uvg.cc3087.myapp.ui.state.DraftLoadError
import com.uvg.cc3087.myapp.ui.state.DraftSaveStatus
import java.util.UUID
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditFormViewModel(
    private val templateRepository: TemplateRepository,
    private val savedStateHandle: SavedStateHandle,
    private val formRepository: FormRepository,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<EditFormUiState> = mutableUiState.asStateFlow()
    private var createdAt = savedStateHandle.get<Long>(CREATED_AT_KEY) ?: 0L
    private var revision = 0L
    private val requests = Channel<DraftRequest>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            // las lecturas esperan los guardados anteriores para no reabrir una versión vieja
            for (request in requests) {
                when (request) {
                    is DraftRequest.Save -> save(request)
                    is DraftRequest.Load -> load(request)
                }
            }
        }
        val pendingId = savedStateHandle.get<String>(ID_KEY)
        if (savedStateHandle.get<Boolean>(LOADING_KEY) == true && pendingId != null) {
            openDraft(pendingId)
        } else if (savedStateHandle.contains(TITLE_KEY)) {
            // recuperamos también las ediciones que android conservó antes de terminar una escritura
            if (!savedStateHandle.contains(CREATED_AT_KEY)) createdAt = now()
            updateState(mutableUiState.value.copy(draftId = pendingId ?: newId()))
        }
    }

    fun startDraft(title: String, templateType: TemplateType?) {
        // la ui pasa la selección y aquí obtenemos los datos para iniciar el editor
        val fields = templateType?.let(templateRepository::getInitialFields).orEmpty()
        // entrar desde una plantilla crea un borrador nuevo, aunque el título sea el mismo
        createdAt = now()
        updateState(EditFormUiState(
            title = title,
            fields = fields.map { it.copy(options = it.options.toList()) },
            draftId = newId()
        ))
    }

    fun openDraft(id: String) {
        revision++
        mutableUiState.value = EditFormUiState(draftId = id, isLoading = true)
        savedStateHandle[ID_KEY] = id
        savedStateHandle[LOADING_KEY] = true
        savedStateHandle.remove<String>(TITLE_KEY)
        savedStateHandle.remove<ArrayList<FormFieldDraft>>(FIELDS_V2_KEY)
        savedStateHandle.remove<ArrayList<String>>(FIELDS_KEY)
        requests.trySend(DraftRequest.Load(id, revision))
    }

    fun retrySave() {
        val current = mutableUiState.value
        if (current.saveStatus == DraftSaveStatus.ERROR) updateState(current)
    }

    fun retryLoad() {
        val current = mutableUiState.value
        if (current.loadError != null) current.draftId?.let(::openDraft)
    }

    fun onAction(action: EditFormAction) {
        val current = mutableUiState.value
        if (current.isLoading || current.loadError != null || current.draftId == null) return
        val updated = when (action) {
            is EditFormAction.ChangeTitle -> current.copy(title = action.title)
            is EditFormAction.AddField -> current.copy(
                fields = current.fields + FormFieldDraft(
                    id = "field-${UUID.randomUUID()}",
                    type = action.type,
                    // conservamos el texto recibido, sin buscar recursos ni decidir el idioma
                    title = action.initialTitle,
                    includeTime = false,
                    allowDecimals = false
                )
            )
            is EditFormAction.UpdateField -> current.copy(
                fields = current.fields.map { field ->
                    if (field.id == action.field.id) {
                        action.field.copy(options = action.field.options.toList())
                    } else field
                }
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

    fun previewConfigurationErrors(): Map<String, Set<FieldError>> {
        val current = mutableUiState.value
        return buildMap {
            current.fields.forEach { field ->
                val errors = field.configurationErrors()
                if (errors.isNotEmpty()) put(field.id, errors)
            }
        }
    }

    fun validatePreviewAnswers(
        answers: Map<String, FormAnswer>,
        attempted: Boolean,
        locale: Locale = Locale.getDefault()
    ): Map<String, FieldError> {
        if (!attempted) return emptyMap()
        return mutableUiState.value.fields.mapNotNull { field ->
            field.answerError(answers[field.id] ?: FormAnswer(), LocalDate.now(), locale)?.let { field.id to it }
        }.toMap()
    }

    fun canAdvanceToSaveShare(): Boolean =
        mutableUiState.value.saveStatus == DraftSaveStatus.SAVED && validate() == FormValidationResult.READY

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
        val id = updated.draftId ?: return
        revision++
        val saving = updated.copy(saveStatus = DraftSaveStatus.SAVING, isLoading = false, loadError = null)
        rememberDraft(saving)
        mutableUiState.value = saving
        requests.trySend(DraftRequest.Save(
            FormDraft(id, saving.title, saving.fields, createdAt, now()), revision
        ))
    }

    private fun rememberDraft(updated: EditFormUiState) {
        savedStateHandle[ID_KEY] = updated.draftId
        savedStateHandle[CREATED_AT_KEY] = createdAt
        savedStateHandle[LOADING_KEY] = false
        savedStateHandle[TITLE_KEY] = updated.title
        // el modelo ya es serializable y así no perdemos opciones ni ajustes al restaurar
        savedStateHandle[FIELDS_V2_KEY] = ArrayList(
            updated.fields.map { it.copy(options = it.options.toList()) }
        )
        savedStateHandle.remove<ArrayList<String>>(FIELDS_KEY)
    }

    private suspend fun save(request: DraftRequest.Save) {
        val result = try {
            formRepository.saveDraft(request.draft)
            DraftSaveStatus.SAVED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            DraftSaveStatus.ERROR
        }
        // una escritura anterior no puede marcar como guardada una edición más reciente
        if (revision == request.revision && mutableUiState.value.draftId == request.draft.id) {
            mutableUiState.value = mutableUiState.value.copy(saveStatus = result)
        }
    }

    private suspend fun load(request: DraftRequest.Load) {
        try {
            val draft = formRepository.getDraft(request.id)
            if (revision != request.revision) return
            if (draft == null) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false, loadError = DraftLoadError.NOT_FOUND
                )
            } else {
                createdAt = draft.createdAt
                val loaded = EditFormUiState(
                    title = draft.title, fields = draft.fields, draftId = draft.id,
                    saveStatus = DraftSaveStatus.SAVED
                )
                rememberDraft(loaded)
                mutableUiState.value = loaded
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (revision == request.revision) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false, loadError = DraftLoadError.FAILED
                )
            }
        }
    }

    override fun onCleared() {
        requests.close()
    }

    private fun newId() = "draft-${UUID.randomUUID()}"

    private sealed interface DraftRequest {
        data class Save(val draft: FormDraft, val revision: Long) : DraftRequest
        data class Load(val id: String, val revision: Long) : DraftRequest
    }

    private fun restoreState(): EditFormUiState {
        val title = savedStateHandle.get<String>(TITLE_KEY).orEmpty()
        val id = savedStateHandle.get<String>(ID_KEY)
        if (savedStateHandle.contains(FIELDS_V2_KEY)) {
            val savedFields = savedStateHandle.get<ArrayList<*>>(FIELDS_V2_KEY).orEmpty()
            val fields = savedFields.filterIsInstance<FormFieldDraft>()
            return EditFormUiState(title = title, fields = fields, draftId = id)
        }
        // todavía aceptamos los borradores de cuatro valores de la versión anterior
        val values = savedStateHandle.get<ArrayList<String>>(FIELDS_KEY).orEmpty()
        if (values.size % FIELD_VALUE_COUNT != 0) return EditFormUiState(title = title, draftId = id)

        val fields = values.chunked(FIELD_VALUE_COUNT).map { fieldValues ->
            val type = FormFieldType.entries.firstOrNull { it.name == fieldValues[1] }
                ?: return EditFormUiState(title = title, draftId = id)
            val required = fieldValues[3].toBooleanStrictOrNull()
                ?: return EditFormUiState(title = title, draftId = id)
            FormFieldDraft(
                id = fieldValues[0],
                type = type,
                title = fieldValues[2],
                required = required
            )
        }
        return EditFormUiState(title = title, fields = fields, draftId = id)
    }

    companion object {
        private const val TITLE_KEY = "editor_title"
        private const val FIELDS_KEY = "editor_fields"
        private const val FIELDS_V2_KEY = "editor_fields_v2"
        private const val FIELD_VALUE_COUNT = 4
        private const val ID_KEY = "editor_draft_id"
        private const val CREATED_AT_KEY = "editor_created_at"
        private const val LOADING_KEY = "editor_loading_draft"

        fun factory(templateRepository: TemplateRepository, formRepository: FormRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditFormViewModel(templateRepository, createSavedStateHandle(), formRepository) }
        }
    }
}
