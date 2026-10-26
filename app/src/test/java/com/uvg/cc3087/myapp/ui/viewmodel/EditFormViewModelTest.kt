package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.FormAnswer
import com.uvg.cc3087.myapp.data.model.FieldError
import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.withSelectionMode
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.testing.FakeTemplateRepository
import com.uvg.cc3087.myapp.testing.FakeFormRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.ui.state.EditFormAction
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.state.FieldMoveDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.After
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate
import java.util.Locale

class EditFormViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val viewModels = ViewModelStore()
    private var viewModelNumber = 0

    @After fun clearViewModels() { viewModels.clear() }

    private val fields = listOf(
        FormFieldDraft("first", FormFieldType.TEXT, "Nombre", required = true),
        FormFieldDraft("middle", FormFieldType.MULTIPLE_CHOICE, "Pedido"),
        FormFieldDraft("last", FormFieldType.DATE, "Fecha")
    )

    @Test
    fun startingBlankDraftKeepsTheTitleAndNoFields() {
        val repository = templateRepository()
        val viewModel = createViewModel(repository, SavedStateHandle())

        viewModel.startDraft("Sin título", null)

        assertEquals("Sin título", viewModel.uiState.value.title)
        assertTrue(viewModel.uiState.value.fields.isEmpty())
        assertTrue(repository.requestedTemplateTypes.isEmpty())
    }

    @Test
    fun previewValidationIsOwnedByTheViewModel() {
        val viewModel = editor()
        viewModel.onAction(EditFormAction.DeleteField("first"))
        viewModel.onAction(EditFormAction.DeleteField("middle"))
        viewModel.onAction(EditFormAction.DeleteField("last"))
        viewModel.onAction(EditFormAction.AddField(FormFieldType.MULTIPLE_CHOICE, ""))
        val broken = viewModel.uiState.value.fields.single()
        viewModel.onAction(EditFormAction.UpdateField(broken.copy(options = emptyList())))
        val configErrors = viewModel.previewConfigurationErrors()
        assertEquals(setOf(FieldError.MISSING_QUESTION, FieldError.TOO_FEW_OPTIONS), configErrors[broken.id])

        viewModel.onAction(EditFormAction.UpdateField(broken.copy(type = FormFieldType.TEXT, title = "Pregunta", required = true)))
        val fieldId = viewModel.uiState.value.fields.single().id
        val errors = viewModel.validatePreviewAnswers(mapOf(fieldId to FormAnswer()), true, java.util.Locale.US)
        assertEquals(FieldError.REQUIRED, errors[fieldId])
        assertTrue(viewModel.validatePreviewAnswers(mapOf(fieldId to FormAnswer(text = "ok")), true, java.util.Locale.US).isEmpty())
    }

    @Test
    fun saveAndShareRequiresAValidDraftThatIsAlreadySaved() {
        val viewModel = editor()
        assertFalse(viewModel.canAdvanceToSaveShare())
        viewModel.onAction(EditFormAction.ChangeTitle("Pedido"))
        viewModel.onAction(EditFormAction.AddField(FormFieldType.TEXT, "Nombre"))
        assertFalse(viewModel.canAdvanceToSaveShare())
    }

    @Test
    fun previewValidatesPastDatesUsingTheInjectedCalendar() {
        val date = FormFieldDraft("date", FormFieldType.DATE, "Fecha", includeTime = false, disallowPastDates = true)
        val viewModel = EditFormViewModel(
            templateRepository(listOf(date)), SavedStateHandle(), FakeFormRepository(),
            today = { LocalDate.of(2026, 10, 26) }
        ).also { viewModels.put("calendar-editor", it) }
        viewModel.startDraft("Reserva", TemplateType.ORDER_FORM)

        assertEquals(
            mapOf("date" to FieldError.PAST_DATE),
            viewModel.validatePreviewAnswers(mapOf("date" to FormAnswer(date = "2026-10-25")), true, Locale.US)
        )
        assertTrue(viewModel.validatePreviewAnswers(
            mapOf("date" to FormAnswer(date = "2026-10-26")), true, Locale.US
        ).isEmpty())
    }

    @Test
    fun startingFromTemplateCopiesTheFieldsWithoutChangingTheSource() {
        val sourceFields = fields.toMutableList()
        val viewModel = createViewModel(templateRepository(sourceFields), SavedStateHandle())
        viewModel.startDraft("Pedido", TemplateType.ORDER_FORM)

        sourceFields.clear()

        assertEquals(fields, viewModel.uiState.value.fields)
    }

    @Test
    fun changingTheTitleUpdatesItsErrorState() {
        val viewModel = editor()

        viewModel.onAction(EditFormAction.ChangeTitle("   "))
        assertTrue(viewModel.uiState.value.hasTitleError)

        viewModel.onAction(EditFormAction.ChangeTitle("Nuevo pedido"))
        assertEquals("Nuevo pedido", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.hasTitleError)
    }

    @Test
    fun addingEachFieldTypeCreatesUniqueIdsAndKeepsTheProvidedTitles() {
        val viewModel = editor(initialFields = emptyList())
        val initialTitles = FormFieldType.entries.map { "Question: ${it.name}" }

        FormFieldType.entries.zip(initialTitles).forEach { (type, title) ->
            viewModel.onAction(EditFormAction.AddField(type, title))
        }

        val addedFields = viewModel.uiState.value.fields
        assertEquals(FormFieldType.entries, addedFields.map { it.type })
        assertEquals(initialTitles, addedFields.map { it.title })
        assertEquals(FormFieldType.entries.size, addedFields.map { it.id }.distinct().size)
        assertTrue(addedFields.none { it.required })
        assertTrue(addedFields.none { it.includeTime || it.allowDecimals })
    }

    @Test
    fun deletingAndAddingAFieldDoesNotReuseItsId() {
        val viewModel = editor(initialFields = emptyList())
        viewModel.onAction(EditFormAction.AddField(FormFieldType.TEXT, "Pregunta de texto"))
        val previousId = viewModel.uiState.value.fields.single().id

        viewModel.onAction(EditFormAction.DeleteField(previousId))
        viewModel.onAction(EditFormAction.AddField(FormFieldType.TEXT, "Pregunta de texto"))

        assertNotEquals(previousId, viewModel.uiState.value.fields.single().id)
    }

    @Test
    fun changingAFieldTitleOnlyUpdatesThatFieldAndItsError() {
        val viewModel = editor()

        viewModel.onAction(EditFormAction.ChangeFieldTitle("middle", ""))
        assertEquals(setOf("middle"), viewModel.uiState.value.fieldIdsWithTitleErrors)

        viewModel.onAction(EditFormAction.ChangeFieldTitle("middle", "Producto"))
        assertTrue(viewModel.uiState.value.fieldIdsWithTitleErrors.isEmpty())
        assertEquals(fields[0], viewModel.uiState.value.fields[0])
        assertEquals(fields[1].copy(title = "Producto"), viewModel.uiState.value.fields[1])
        assertEquals(fields[2], viewModel.uiState.value.fields[2])
    }

    @Test
    fun changingRequiredOnlyUpdatesTheChosenField() {
        val viewModel = editor()

        viewModel.onAction(EditFormAction.SetFieldRequired("middle", true))
        viewModel.onAction(EditFormAction.SetFieldRequired("first", false))

        assertEquals(
            listOf(fields[0].copy(required = false), fields[1].copy(required = true), fields[2]),
            viewModel.uiState.value.fields
        )
    }

    @Test
    fun movingUpPreservesTheFieldIdentityAndValues() {
        val viewModel = editor()

        viewModel.onAction(EditFormAction.MoveField("middle", FieldMoveDirection.UP))

        assertEquals(listOf(fields[1], fields[0], fields[2]), viewModel.uiState.value.fields)
    }

    @Test
    fun movingDownUsesTheIdEvenAfterTheOrderChanges() {
        val viewModel = editor()

        viewModel.onAction(EditFormAction.MoveField("first", FieldMoveDirection.DOWN))
        viewModel.onAction(EditFormAction.MoveField("first", FieldMoveDirection.DOWN))

        assertEquals(listOf(fields[1], fields[2], fields[0]), viewModel.uiState.value.fields)
    }

    @Test
    fun movingPastTheListEdgesDoesNothing() {
        val viewModel = editor()

        viewModel.onAction(EditFormAction.MoveField("first", FieldMoveDirection.UP))
        viewModel.onAction(EditFormAction.MoveField("last", FieldMoveDirection.DOWN))

        assertEquals(fields, viewModel.uiState.value.fields)
    }

    @Test
    fun actionsForAnUnknownFieldDoNotChangeTheDraft() {
        val viewModel = editor()
        val before = viewModel.uiState.value

        viewModel.onAction(EditFormAction.ChangeFieldTitle("missing", "Otro"))
        viewModel.onAction(EditFormAction.SetFieldRequired("missing", true))
        viewModel.onAction(EditFormAction.MoveField("missing", FieldMoveDirection.UP))
        viewModel.onAction(EditFormAction.DeleteField("missing"))

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun deletingAFieldKeepsTheOtherFieldsInOrder() {
        val viewModel = editor()

        viewModel.onAction(EditFormAction.DeleteField("middle"))

        assertEquals(listOf(fields[0], fields[2]), viewModel.uiState.value.fields)
    }

    @Test
    fun startingAnotherDraftResetsThePreviousEditsEvenWithTheSameTitle() {
        val viewModel = editor()
        viewModel.onAction(EditFormAction.ChangeFieldTitle("first", "Editado"))
        viewModel.onAction(EditFormAction.AddField(FormFieldType.DATE, "Selecciona una fecha"))

        viewModel.startDraft("Pedido", TemplateType.ORDER_FORM)

        assertEquals("Pedido", viewModel.uiState.value.title)
        assertEquals(fields, viewModel.uiState.value.fields)
    }

    @Test
    fun savedValuesRestoreTheTitleFieldsOrderIdsAndRequiredFlags() {
        val savedStateHandle = SavedStateHandle()
        val original = createViewModel(templateRepository(), savedStateHandle)
        original.startDraft("Pedido", TemplateType.ORDER_FORM)
        original.onAction(EditFormAction.ChangeTitle("Pedido\n\"Especial\""))
        original.onAction(EditFormAction.ChangeFieldTitle("first", "Nombre actualizado"))
        original.onAction(EditFormAction.SetFieldRequired("last", true))
        original.onAction(EditFormAction.MoveField("last", FieldMoveDirection.UP))
        original.onAction(EditFormAction.AddField(FormFieldType.MULTIPLE_CHOICE, "Choose an option"))

        // recreamos el viewmodel con los valores guardados, no con su instancia anterior
        val savedValues = savedStateHandle.keys().associateWith { key ->
            savedStateHandle.get<Any?>(key)
        }
        val restoredRepository = templateRepository()
        val restored = createViewModel(restoredRepository, SavedStateHandle(savedValues))

        assertEquals(original.uiState.value, restored.uiState.value)
        assertTrue(restoredRepository.requestedTemplateTypes.isEmpty())
        val restoredIds = restored.uiState.value.fields.map { it.id }
        restored.onAction(EditFormAction.AddField(FormFieldType.TEXT, "Text question"))
        assertEquals(restoredIds, restored.uiState.value.fields.dropLast(1).map { it.id })
        assertEquals(5, restored.uiState.value.fields.map { it.id }.distinct().size)
    }

    @Test
    fun validationAlwaysUsesTheCurrentDraft() {
        val viewModel = editor(title = "", initialFields = emptyList())
        assertEquals(FormValidationResult.TITLE_REQUIRED, viewModel.validate())

        viewModel.onAction(EditFormAction.ChangeTitle("Pedido"))
        assertEquals(FormValidationResult.FIELD_REQUIRED, viewModel.validate())

        viewModel.onAction(EditFormAction.AddField(FormFieldType.TEXT, "Pregunta de texto"))
        assertEquals(FormValidationResult.READY, viewModel.validate())

        val fieldId = viewModel.uiState.value.fields.single().id
        viewModel.onAction(EditFormAction.ChangeFieldTitle(fieldId, "   "))
        assertEquals(FormValidationResult.FIELD_TITLE_REQUIRED, viewModel.validate())
    }

    @Test
    fun anIncompleteSavedFieldRecordDoesNotCrashRestoration() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "editor_title" to "Pedido recuperado",
                "editor_fields" to arrayListOf("id", "TEXT", "Nombre")
            )
        )

        val viewModel = createViewModel(templateRepository(), savedStateHandle)

        assertEquals("Pedido recuperado", viewModel.uiState.value.title)
        assertTrue(viewModel.uiState.value.fields.isEmpty())
    }

    @Test
    fun creatingTheViewModelDoesNotStartATemplateDraft() {
        val repository = templateRepository()

        val viewModel = createViewModel(repository, SavedStateHandle())

        assertEquals(EditFormUiState(), viewModel.uiState.value)
        assertTrue(repository.requestedTemplateTypes.isEmpty())
    }

    @Test
    fun theSelectedTemplateTypeChoosesTheFieldsNotTheDisplayedTitle() {
        val repository = templateRepository()
        val viewModel = createViewModel(repository, SavedStateHandle())

        viewModel.startDraft("Título elegido por el usuario", TemplateType.FEEDBACK)

        assertEquals(listOf(TemplateType.FEEDBACK), repository.requestedTemplateTypes)
        assertEquals("Título elegido por el usuario", viewModel.uiState.value.title)
        assertEquals(fields, viewModel.uiState.value.fields)
    }

    @Test
    fun changingTemplatesWithTheSameTitleUsesTheNewRepositoryFields() {
        val feedbackFields = listOf(FormFieldDraft("feedback-1", FormFieldType.TEXT, "Comentario"))
        val repository = FakeTemplateRepository(
            initialFieldsByTemplate = mapOf(
                TemplateType.ORDER_FORM to fields,
                TemplateType.FEEDBACK to feedbackFields
            )
        )
        val viewModel = createViewModel(repository, SavedStateHandle())
        viewModel.startDraft("Formulario", TemplateType.ORDER_FORM)
        viewModel.onAction(EditFormAction.ChangeFieldTitle("first", "Editado"))

        viewModel.startDraft("Formulario", TemplateType.FEEDBACK)

        assertEquals(
            listOf(TemplateType.ORDER_FORM, TemplateType.FEEDBACK),
            repository.requestedTemplateTypes
        )
        assertEquals("Formulario", viewModel.uiState.value.title)
        assertEquals(feedbackFields, viewModel.uiState.value.fields)
        assertEquals("Nombre", fields.first().title)
    }

    @Test
    fun aTemplateWithoutFieldsDoesNotFallBackToSampleData() {
        val repository = FakeTemplateRepository()
        val viewModel = createViewModel(repository, SavedStateHandle())

        viewModel.startDraft("Plantilla vacía", TemplateType.EVENT_RSVP)

        assertEquals(listOf(TemplateType.EVENT_RSVP), repository.requestedTemplateTypes)
        assertTrue(viewModel.uiState.value.fields.isEmpty())
        assertEquals(FormValidationResult.FIELD_REQUIRED, viewModel.validate())
    }

    @Test
    fun theProvidedInitialTitleIsPreservedWhenTheDraftIsRestored() {
        val savedStateHandle = SavedStateHandle()
        val original = createViewModel(templateRepository(), savedStateHandle)
        original.startDraft("Formulario", null)
        val initialTitle = "Question: ¿cuál prefieres?"

        original.onAction(EditFormAction.AddField(FormFieldType.TEXT, initialTitle))

        val savedValues = savedStateHandle.keys().associateWith { key ->
            savedStateHandle.get<Any?>(key)
        }
        val restored = createViewModel(templateRepository(), SavedStateHandle(savedValues))

        assertEquals(initialTitle, original.uiState.value.fields.single().title)
        assertEquals(original.uiState.value, restored.uiState.value)
    }

    @Test
    fun anEmptyInitialTitleIsValidatedInsteadOfBeingReplacedByHardcodedText() {
        val viewModel = editor(initialFields = emptyList())

        viewModel.onAction(EditFormAction.AddField(FormFieldType.TEXT, ""))

        val field = viewModel.uiState.value.fields.single()
        assertEquals("", field.title)
        assertEquals(setOf(field.id), viewModel.uiState.value.fieldIdsWithTitleErrors)
        assertEquals(FormValidationResult.FIELD_TITLE_REQUIRED, viewModel.validate())
    }

    @Test
    fun updatingAFieldKeepsAllItsConfigurationAndTheOtherFields() {
        val viewModel = editor()
        val changed = fields[1].copy(
            options = listOf(FieldOption("a", "Café"), FieldOption("b", "Té")),
            helpText = "Puedes elegir varias",
            placeholder = "Tu respuesta",
            required = true
        ).withSelectionMode(true)

        viewModel.onAction(EditFormAction.UpdateField(changed))

        assertEquals(listOf(fields[0], changed, fields[2]), viewModel.uiState.value.fields)
        assertEquals(FormValidationResult.READY, viewModel.validate())
    }

    @Test
    fun updatingAnUnknownFieldDoesNotInsertIt() {
        val viewModel = editor()
        val before = viewModel.uiState.value
        viewModel.onAction(EditFormAction.UpdateField(fields[0].copy(id = "missing")))
        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun serializedSavedStateRestoresEveryConfigurationAndOptionIdentity() {
        val saved = SavedStateHandle()
        val original = createViewModel(templateRepository(), saved)
        original.startDraft("Encuesta", TemplateType.ORDER_FORM)
        original.onAction(EditFormAction.UpdateField(fields[1].copy(
            options = listOf(FieldOption("coffee", "Café"), FieldOption("tea", "Té")),
            helpText = "Elige tus bebidas",
            placeholder = "Aquí",
            required = true
        ).withSelectionMode(true)))
        original.onAction(EditFormAction.UpdateField(fields[2].copy(
            includeTime = true, disallowPastDates = true
        )))
        original.onAction(EditFormAction.AddField(FormFieldType.NUMBER))
        val number = original.uiState.value.fields.last()
        original.onAction(EditFormAction.UpdateField(number.copy(
            title = "Cantidad", allowDecimals = false, minimum = "-2",
            maximum = "15", unit = "unidades"
        )))

        // pasamos por serialización real para no reutilizar las instancias originales
        val values = HashMap(saved.keys().associateWith { saved.get<Any?>(it) })
        val bytes = ByteArrayOutputStream().also { output ->
            ObjectOutputStream(output).use { it.writeObject(values) }
        }.toByteArray()
        @Suppress("UNCHECKED_CAST")
        val restoredValues = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as Map<String, Any?>
        }
        val restored = createViewModel(templateRepository(), SavedStateHandle(restoredValues))

        assertEquals(original.uiState.value, restored.uiState.value)
        assertEquals(listOf("coffee", "tea"), restored.uiState.value.fields[1].options.map { it.id })
    }

    @Test
    fun legacyFourValueRecordsStillRestoreAndMigrateOnEdit() {
        val saved = SavedStateHandle(mapOf(
            "editor_title" to "Anterior",
            "editor_fields" to arrayListOf("old", "TEXT", "Nombre", "true")
        ))
        val viewModel = createViewModel(templateRepository(), saved)
        assertEquals(listOf(FormFieldDraft("old", FormFieldType.TEXT, "Nombre", true)), viewModel.uiState.value.fields)
        viewModel.onAction(EditFormAction.ChangeTitle("Actualizado"))
        assertTrue(saved.contains("editor_fields_v2"))
        assertFalse(saved.contains("editor_fields"))
    }

    @Test
    fun blankOptionsAreRejectedUntilTheChoicesAreCompleted() {
        val viewModel = editor(initialFields = emptyList())
        viewModel.onAction(EditFormAction.AddField(FormFieldType.MULTIPLE_CHOICE, "Bebida"))
        assertEquals(FormValidationResult.FIELD_CONFIGURATION_INVALID, viewModel.validate())
        val field = viewModel.uiState.value.fields.single()
        viewModel.onAction(EditFormAction.UpdateField(field.copy(
            options = listOf(FieldOption("a", "Café"), FieldOption("b", "Té"))
        )))
        assertEquals(FormValidationResult.READY, viewModel.validate())
    }

    private fun editor(
        title: String = "Pedido",
        initialFields: List<FormFieldDraft> = fields
    ) = createViewModel(templateRepository(initialFields), SavedStateHandle()).apply {
        startDraft(title, TemplateType.ORDER_FORM)
    }

    private fun templateRepository(initialFields: List<FormFieldDraft> = fields) = FakeTemplateRepository(
        initialFieldsByTemplate = TemplateType.entries.associateWith { initialFields }
    )

    private fun createViewModel(templates: TemplateRepository, saved: SavedStateHandle) =
        EditFormViewModel(templates, saved, FakeFormRepository()).also {
            viewModels.put("editor-${viewModelNumber++}", it)
        }
}
