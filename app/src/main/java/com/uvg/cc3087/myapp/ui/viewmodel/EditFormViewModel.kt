package com.uvg.cc3087.myapp.ui.viewmodel

import com.uvg.cc3087.myapp.domain.telemetry.*
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
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.CoverStorage
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
    private val now: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
    private val covers: CoverStorage? = null,
    private val analytics: ProductAnalytics = NoProductAnalytics
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<EditFormUiState> = mutableUiState.asStateFlow()
    private var createdAt = savedStateHandle.get<Long>(CREATED_AT_KEY) ?: 0L
    private var creationAttempt = savedStateHandle.get<String>("creation_attempt")
    private var creationStart = savedStateHandle.get<Long>("creation_start")
    private var templateName = savedStateHandle.get<String>("creation_template")
    private var revision = 0L
    private val requests = Channel<DraftRequest>(Channel.UNLIMITED)

    private var coverObservation: kotlinx.coroutines.Job? = null

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
            observeCover(checkNotNull(mutableUiState.value.draftId))
        }
    }

    fun beginCreationAttempt() {
        val id = newId()
        val attempt = UUID.randomUUID().toString()
        savedStateHandle["pending_creation_id"] = id
        savedStateHandle["pending_creation_attempt"] = attempt
        savedStateHandle["pending_creation_start"] = now()
        viewModelScope.launch { analytics.creationStarted(id, attempt) }
    }

    fun startDraft(title: String, templateType: TemplateType?) {
        coverObservation?.cancel()
        if (!savedStateHandle.contains("pending_creation_id")) beginCreationAttempt()
        val id = checkNotNull(savedStateHandle.remove<String>("pending_creation_id"))
        creationAttempt = savedStateHandle.remove<String>("pending_creation_attempt")
        creationStart = savedStateHandle.remove<Long>("pending_creation_start")
        templateName = templateType?.name ?: "CUSTOM"
        // la ui pasa la selección y aquí obtenemos los datos para iniciar el editor
        val fields = templateType?.let(templateRepository::getInitialFields).orEmpty()
        // entrar desde una plantilla crea un borrador nuevo, aunque el título sea el mismo
        createdAt = now()
        updateState(EditFormUiState(
            title = title,
            fields = fields.map { it.copy(options = it.options.toList()) },
            draftId = id
        ))
    }

    fun openDraft(id: String) {
        val current = mutableUiState.value
        // volver desde forms no debe reemplazar una edición pendiente por la copia de room
        if (current.draftId == id &&
            (current.saveStatus == DraftSaveStatus.SAVING || current.saveStatus == DraftSaveStatus.ERROR)
        ) return
        coverObservation?.cancel()
        revision++
        mutableUiState.value = EditFormUiState(draftId = id, isLoading = true)
        savedStateHandle[ID_KEY] = id
        savedStateHandle[LOADING_KEY] = true
        savedStateHandle.remove<String>(OWNER_KEY)
        savedStateHandle.remove<String>(COVER_KEY)
        savedStateHandle.remove<String>(COVER_ACTION_KEY)
        savedStateHandle.remove<String>(PUBLISHED_COVER_KEY)
        savedStateHandle.remove<String>(TITLE_KEY)
        savedStateHandle.remove<ArrayList<FormFieldDraft>>(FIELDS_V2_KEY)
        savedStateHandle.remove<ArrayList<String>>(FIELDS_KEY)
        requests.trySend(DraftRequest.Load(id, revision))
    }

    private var coverRevision = 0L

    fun importCover(source: String) {
        val current = mutableUiState.value
        val id = current.draftId ?: return
        if (current.isLoading || current.loadError != null) return
        val token = ++coverRevision
        mutableUiState.value = current.copy(importingCover = true, coverError = false)
        viewModelScope.launch {
            try {
                val path = checkNotNull(covers).importImage(source)
                if (token == coverRevision && mutableUiState.value.draftId == id) {
                    updateState(mutableUiState.value.copy(coverPath = path, coverAction = "REPLACE", importingCover = false))
                } else covers.delete(path)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (token == coverRevision && mutableUiState.value.draftId == id)
                    mutableUiState.value = mutableUiState.value.copy(importingCover = false, coverError = true)
            }
        }
    }

    fun removeCover() {
        coverRevision++
        val state = mutableUiState.value
        if (!state.isLoading && state.loadError == null)
            updateState(state.copy(coverPath = null, coverAction = "REMOVE", publishedCoverUrl = null, importingCover = false, coverError = false))
    }

    fun requestContinue(): FormValidationResult {
        val result = validate()
        val state = mutableUiState.value
        if (result == FormValidationResult.READY && !state.importingCover && state.loadError == null && !state.isLoading) {
            mutableUiState.value = state.copy(continueRequested = true, readyToContinue = canAdvanceToSaveShare())
        }
        return result
    }

    fun consumeContinue() {
        mutableUiState.value = mutableUiState.value.copy(continueRequested = false, readyToContinue = false)
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

    fun previewConfigurationErrors(): Map<String, Set<FieldError>> =
        FormDraftValidator.configurationErrors(mutableUiState.value.fields)

    fun validatePreviewAnswers(
        answers: Map<String, FormAnswer>,
        attempted: Boolean,
        locale: Locale = Locale.getDefault()
    ): Map<String, FieldError> = FormDraftValidator.validateAnswers(
        mutableUiState.value.fields, answers, attempted, today(), locale
    )

    fun canAdvanceToSaveShare(): Boolean {
        val current = mutableUiState.value
        return current.draftId != null && !current.isLoading && current.loadError == null &&
            !current.importingCover && current.saveStatus == DraftSaveStatus.SAVED && validate() == FormValidationResult.READY
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
        val id = updated.draftId ?: return
        revision++
        val saving = updated.copy(saveStatus = DraftSaveStatus.SAVING, isLoading = false, loadError = null)
        rememberDraft(saving)
        mutableUiState.value = saving
        requests.trySend(DraftRequest.Save(
            FormDraft(id, saving.title, saving.fields, createdAt, now(), saving.coverPath, saving.ownerId, creationAttempt, creationStart, templateName, saving.coverAction), revision
        ))
    }

    private fun rememberDraft(updated: EditFormUiState) {
        savedStateHandle[ID_KEY] = updated.draftId
        savedStateHandle[CREATED_AT_KEY] = createdAt
        savedStateHandle["creation_attempt"] = creationAttempt
        savedStateHandle["creation_start"] = creationStart
        savedStateHandle["creation_template"] = templateName
        savedStateHandle[LOADING_KEY] = false
        savedStateHandle[TITLE_KEY] = updated.title
        savedStateHandle[COVER_KEY] = updated.coverPath
        savedStateHandle[COVER_ACTION_KEY] = updated.coverAction
        savedStateHandle[PUBLISHED_COVER_KEY] = updated.publishedCoverUrl
        savedStateHandle[OWNER_KEY] = updated.ownerId
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
            val state = mutableUiState.value
            mutableUiState.value = state.copy(saveStatus = result,
                readyToContinue = state.continueRequested && result == DraftSaveStatus.SAVED &&
                    !state.importingCover && validate() == FormValidationResult.READY)
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
                creationAttempt = draft.creationAttemptId
                creationStart = draft.creationStartedAt
                templateName = draft.templateType
                if (revision != request.revision) return
                val loaded = EditFormUiState(
                    title = draft.title, fields = draft.fields, draftId = draft.id, coverPath = draft.coverPath, ownerId = draft.ownerId,
                    coverAction = draft.coverAction,
                    saveStatus = DraftSaveStatus.SAVED
                )
                rememberDraft(loaded)
                mutableUiState.value = loaded
                observeCover(draft.id)
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

    private fun observeCover(id: String) {
        coverObservation?.cancel()
        coverObservation = viewModelScope.launch {
            formRepository.observePublishedCoverUrl(id).collect { url ->
                val current = mutableUiState.value
                if (current.draftId == id) {
                    // Publication metadata changes presentation only, never the pending draft revision.
                    mutableUiState.value = current.copy(publishedCoverUrl = url)
                    savedStateHandle[PUBLISHED_COVER_KEY] = url
                }
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
            return EditFormUiState(title = title, fields = fields, draftId = id, coverPath = savedStateHandle[COVER_KEY], coverAction = savedStateHandle[COVER_ACTION_KEY] ?: "KEEP", publishedCoverUrl = savedStateHandle[PUBLISHED_COVER_KEY], ownerId = savedStateHandle[OWNER_KEY])
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
        return EditFormUiState(title = title, fields = fields, draftId = id, coverPath = savedStateHandle[COVER_KEY], coverAction = savedStateHandle[COVER_ACTION_KEY] ?: "KEEP", publishedCoverUrl = savedStateHandle[PUBLISHED_COVER_KEY], ownerId = savedStateHandle[OWNER_KEY])
    }

    companion object {
        private const val OWNER_KEY = "editor_owner"
        private const val COVER_ACTION_KEY = "editor_cover_action"
        private const val PUBLISHED_COVER_KEY = "editor_published_cover"
        private const val COVER_KEY = "editor_cover"
        private const val TITLE_KEY = "editor_title"
        private const val FIELDS_KEY = "editor_fields"
        private const val FIELDS_V2_KEY = "editor_fields_v2"
        private const val FIELD_VALUE_COUNT = 4
        private const val ID_KEY = "editor_draft_id"
        private const val CREATED_AT_KEY = "editor_created_at"
        private const val LOADING_KEY = "editor_loading_draft"

        fun factory(templateRepository: TemplateRepository, formRepository: FormRepository, covers: CoverStorage? = null, analytics: ProductAnalytics = NoProductAnalytics): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditFormViewModel(templateRepository, createSavedStateHandle(), formRepository, covers = covers, analytics = analytics) }
        }
    }
}
