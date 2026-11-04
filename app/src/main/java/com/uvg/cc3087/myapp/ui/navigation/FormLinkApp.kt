package com.uvg.cc3087.myapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.model.FieldError
import com.uvg.cc3087.myapp.data.model.FormAnswer
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.di.AppContainer
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.resources.titleResId
import com.uvg.cc3087.myapp.ui.screens.ChooseTemplate
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.screens.SaveShareScreen
import com.uvg.cc3087.myapp.ui.state.SaveShareUiState
import com.uvg.cc3087.myapp.ui.state.DraftSaveStatus
import com.uvg.cc3087.myapp.ui.resources.copyFormLink
import com.uvg.cc3087.myapp.ui.resources.shareFormLinkOnWhatsApp
import com.uvg.cc3087.myapp.ui.state.ChooseTemplateUiState
import com.uvg.cc3087.myapp.ui.state.EditFormAction
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.state.FormFilter
import com.uvg.cc3087.myapp.ui.state.FormsUiState
import com.uvg.cc3087.myapp.ui.state.FormsLoadStatus
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.ChooseTemplateViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.EditFormViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.FormsViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.SaveShareViewModel
import java.util.Locale

private enum class AppDestination {
    FORMS,
    CHOOSE_TEMPLATE,
    EDIT_FORM,
    SAVE_SHARE
}

@Composable
fun FormLinkApp() {
    val context = LocalContext.current.applicationContext
    val formRepository = remember(context) { AppContainer.formRepository(context) }
    val resources = LocalResources.current
    val languageTags = resources.configuration.locales.toLanguageTags()
    LaunchedEffect(languageTags) {
        AppContainer.updateTemplateFields(FormEditorSampleData.templateFields(resources))
    }
    FormLinkRoute(formRepository, AppContainer.templateRepository)
}

@Composable
internal fun FormLinkRoute(formRepository: FormRepository, templateRepository: TemplateRepository) {
    val resources = LocalResources.current
    // los viewmodels comparten el repositorio, no una copia de los formularios
    val formsViewModel: FormsViewModel = viewModel(
        factory = FormsViewModel.factory(formRepository)
    )
    val formsUiState by formsViewModel.uiState.collectAsStateWithLifecycle()
    val chooseTemplateViewModel: ChooseTemplateViewModel = viewModel(
        factory = ChooseTemplateViewModel.factory(templateRepository)
    )
    val chooseTemplateUiState by chooseTemplateViewModel.uiState.collectAsStateWithLifecycle()
    val editFormViewModel: EditFormViewModel = viewModel(
        factory = EditFormViewModel.factory(templateRepository, formRepository)
    )
    val editFormUiState by editFormViewModel.uiState.collectAsStateWithLifecycle()
    val saveShareViewModel: SaveShareViewModel = viewModel(factory = SaveShareViewModel.factory(formRepository))
    val saveShareUiState by saveShareViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    FormLinkContent(
        formsUiState = formsUiState,
        chooseTemplateUiState = chooseTemplateUiState,
        editFormUiState = editFormUiState,
        onFilterSelected = formsViewModel::selectFilter,
        onStartEditor = { template ->
            editFormViewModel.startDraft(
                // el idioma se resuelve en la ui y los campos los decide el viewmodel
                title = resources.getString(template?.type?.titleResId ?: R.string.untitled_form),
                templateType = template?.type
            )
        },
        onEditorAction = editFormViewModel::onAction,
        onValidateEditor = editFormViewModel::validate,
        onPreviewConfigurationErrors = editFormViewModel::previewConfigurationErrors,
        onValidatePreviewAnswers = editFormViewModel::validatePreviewAnswers,
        onCanAdvanceToSaveShare = editFormViewModel::canAdvanceToSaveShare,
        onOpenDraft = editFormViewModel::openDraft,
        onRetrySave = editFormViewModel::retrySave,
        onRetryEditorLoad = editFormViewModel::retryLoad,
        onRetryFormsLoad = formsViewModel::retryLoading,
        saveShareUiState = saveShareUiState,
        onOpenSaveShare = saveShareViewModel::openDraft,
        onRetrySaveShareLoad = saveShareViewModel::retryLoading,
        onCopyLink = { copyFormLink(context, it) },
        onWhatsAppShare = { shareFormLinkOnWhatsApp(context, it) }
    )
}

@Composable
private fun FormLinkContent(
    formsUiState: FormsUiState,
    chooseTemplateUiState: ChooseTemplateUiState,
    editFormUiState: EditFormUiState,
    onFilterSelected: (FormFilter) -> Unit,
    onStartEditor: (FormTemplate?) -> Unit,
    onEditorAction: (EditFormAction) -> Unit,
    onValidateEditor: () -> FormValidationResult,
    onPreviewConfigurationErrors: () -> Map<String, Set<FieldError>>,
    onValidatePreviewAnswers: (Map<String, FormAnswer>, Boolean, Locale) -> Map<String, FieldError>,
    onCanAdvanceToSaveShare: () -> Boolean,
    onOpenDraft: (String) -> Unit = {},
    onRetrySave: () -> Unit = {},
    onRetryEditorLoad: () -> Unit = {},
    onRetryFormsLoad: () -> Unit = {},
    saveShareUiState: SaveShareUiState = SaveShareUiState(),
    onOpenSaveShare: (String) -> Unit = {},
    onRetrySaveShareLoad: () -> Unit = {},
    onCopyLink: (String) -> Boolean = { false },
    onWhatsAppShare: (String) -> Boolean = { false }
) {
    // cada pantalla recibe su estado y la navegación sigue aquí
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.FORMS)
    }
    var editorReturnDestination by rememberSaveable {
        mutableStateOf(AppDestination.CHOOSE_TEMPLATE)
    }
    var saveShareReturnDestination by rememberSaveable {
        mutableStateOf(AppDestination.EDIT_FORM)
    }

    val showForms = {
        currentDestination = AppDestination.FORMS
    }

    val showEditor: (FormTemplate?) -> Unit = { template ->
        // solo iniciar desde plantillas reinicia el borrador, no una recomposición o rotación
        onStartEditor(template)
        editorReturnDestination = AppDestination.CHOOSE_TEMPLATE
        currentDestination = AppDestination.EDIT_FORM
    }

    val leaveEditor = { currentDestination = editorReturnDestination }

    // el botón físico sigue el mismo recorrido que las flechas de la app
    BackHandler(enabled = currentDestination != AppDestination.FORMS) {
        when (currentDestination) {
            AppDestination.CHOOSE_TEMPLATE -> showForms()
            AppDestination.EDIT_FORM -> leaveEditor()
            AppDestination.SAVE_SHARE -> { currentDestination = saveShareReturnDestination }
            AppDestination.FORMS -> Unit
        }
    }

    when (currentDestination) {
        AppDestination.FORMS -> {
            Forms(
                uiState = formsUiState,
                onFilterSelected = onFilterSelected,
                onNewFormClick = {
                    currentDestination = AppDestination.CHOOSE_TEMPLATE
                },
                onFormClick = { form ->
                    onOpenDraft(form.id)
                    editorReturnDestination = AppDestination.FORMS
                    currentDestination = AppDestination.EDIT_FORM
                },
                onRetryLoad = onRetryFormsLoad,
                unsavedDraftId = editFormUiState.takeIf { it.saveStatus != DraftSaveStatus.SAVED }?.draftId,
                onShareFormClick = { form ->
                    onOpenSaveShare(form.id)
                    saveShareReturnDestination = AppDestination.FORMS
                    currentDestination = AppDestination.SAVE_SHARE
                }
            )
        }

        AppDestination.CHOOSE_TEMPLATE -> {
            ChooseTemplate(
                uiState = chooseTemplateUiState,
                onBackClick = showForms,
                onBlankFormClick = { showEditor(null) },
                onTemplateClick = showEditor
            )
        }

        AppDestination.EDIT_FORM -> {
            EditForm(
                uiState = editFormUiState,
                onAction = onEditorAction,
                onValidate = onValidateEditor,
                onBackClick = leaveEditor,
                onRetrySave = onRetrySave,
                onRetryLoad = onRetryEditorLoad,
                configurationErrors = onPreviewConfigurationErrors,
                validatePreviewAnswers = onValidatePreviewAnswers,
                canAdvanceToSaveShare = onCanAdvanceToSaveShare,
                onSaveForm = {
                    val id = editFormUiState.draftId
                    if (id != null && onCanAdvanceToSaveShare()) {
                        onOpenSaveShare(id)
                        saveShareReturnDestination = AppDestination.EDIT_FORM
                        currentDestination = AppDestination.SAVE_SHARE
                    }
                }
            )
        }

        AppDestination.SAVE_SHARE -> {
            SaveShareScreen(
                uiState = saveShareUiState,
                onBack = { currentDestination = saveShareReturnDestination },
                onBackToForms = showForms,
                onRetry = onRetrySaveShareLoad,
                onCopyLink = onCopyLink,
                onWhatsAppShare = onWhatsAppShare
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FormLinkAppPreview() {
    MyappTheme(dynamicColor = false) {
        // la preview usa un estado de ejemplo y no necesita crear un viewmodel
        FormLinkContent(
            formsUiState = FormsUiState(forms = FormSampleData.forms, status = FormsLoadStatus.CONTENT),
            chooseTemplateUiState = ChooseTemplateUiState(templates = TemplateSampleData.templates),
            editFormUiState = EditFormUiState(),
            onFilterSelected = {},
            onStartEditor = {},
            onEditorAction = {},
            onValidateEditor = { FormValidationResult.READY },
            onPreviewConfigurationErrors = { emptyMap() },
            onValidatePreviewAnswers = { _, _, _ -> emptyMap() },
            onCanAdvanceToSaveShare = { false }
        )
    }
}
