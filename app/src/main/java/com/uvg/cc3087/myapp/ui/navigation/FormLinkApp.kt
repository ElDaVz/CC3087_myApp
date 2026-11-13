package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
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
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.data.repository.UnavailableRequestRepository
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
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardUiState
import com.uvg.cc3087.myapp.ui.screens.ResponseDashboardScreen
import com.uvg.cc3087.myapp.ui.screens.RequestListScreen
import com.uvg.cc3087.myapp.ui.components.ResponseFilters
import com.uvg.cc3087.myapp.ui.state.RequestListUiState
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import androidx.lifecycle.repeatOnLifecycle

private enum class AppDestination {
    FORMS,
    CHOOSE_TEMPLATE,
    EDIT_FORM,
    SAVE_SHARE,
    DASHBOARD,
    REQUESTS,
    REQUEST_DETAIL
}

@Composable
fun FormLinkApp() {
    val context = LocalContext.current.applicationContext
    val container = remember(context) { AppContainer.also { it.initialize(context) } }
    val resources = LocalResources.current
    val languageTags = resources.configuration.locales.toLanguageTags()
    LaunchedEffect(languageTags) {
        container.updateTemplateFields(FormEditorSampleData.templateFields(resources))
    }
    var initializationAttempt by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    val initialized = androidx.compose.runtime.remember(initializationAttempt) { runCatching { container.backend } }
    if (initialized.isFailure) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            androidx.compose.material3.Text(stringResource(R.string.protected_data_error))
            androidx.compose.material3.Button(onClick = { initializationAttempt++ }) { androidx.compose.material3.Text(stringResource(R.string.retry)) }
        }
        return
    }
    BackendRoot(initialized.getOrThrow(), container.templateRepository,
        container.requestRepository, container.coverStorage)
}

@Composable
internal fun FormLinkRoute(
    formRepository: FormRepository,
    templateRepository: TemplateRepository,
    requestRepository: RequestRepository = UnavailableRequestRepository,
    requestsAreDemo: Boolean = false,
    coverStorage: com.uvg.cc3087.myapp.data.repository.CoverStorage? = null,
    onAccountClick: (() -> Unit)? = null,
    syncMessage: String? = null,
    analytics: com.uvg.cc3087.myapp.domain.telemetry.ProductAnalytics = com.uvg.cc3087.myapp.domain.telemetry.NoProductAnalytics,
    guestImportNotice: @Composable () -> Unit = {}
) {
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
        factory = EditFormViewModel.factory(templateRepository, formRepository, coverStorage, analytics)
    )
    val editFormUiState by editFormViewModel.uiState.collectAsStateWithLifecycle()
    val saveShareViewModel: SaveShareViewModel = viewModel(factory = SaveShareViewModel.factory(formRepository))
    val saveShareUiState by saveShareViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val dashboardModel: ResponseDashboardViewModel = viewModel(factory = ResponseDashboardViewModel.factory(requestRepository))
    val dashboardState by dashboardModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(dashboardModel, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                dashboardModel.checkCalendar()
                kotlinx.coroutines.delay(30_000)
            }
        }
    }
    FormLinkContent(
        guestImportNotice = guestImportNotice,
        dashboardState = dashboardState,
        onDashboardForm = dashboardModel::selectForm,
        onDashboardPeriod = dashboardModel::selectPeriod,
        onDashboardRefresh = dashboardModel::refresh,
        requestRepository = requestRepository,
        onAccountClick = onAccountClick,
        syncMessage = syncMessage,
        requestsAreDemo = requestsAreDemo,
        formsUiState = formsUiState,
        chooseTemplateUiState = chooseTemplateUiState,
        editFormUiState = editFormUiState,
        onFilterSelected = formsViewModel::selectFilter,
        onBeginCreation = editFormViewModel::beginCreationAttempt,
        onStartEditor = { template ->
            editFormViewModel.startDraft(
                // el idioma se resuelve en la ui y los campos los decide el viewmodel
                title = resources.getString(template?.type?.titleResId ?: R.string.untitled_form),
                templateType = template?.type
            )
        },
        onEditorAction = editFormViewModel::onAction,
        onImportCover = editFormViewModel::importCover,
        onRemoveCover = editFormViewModel::removeCover,
        onContinue = editFormViewModel::requestContinue,
        onConsumeContinue = editFormViewModel::consumeContinue,
        onValidateEditor = editFormViewModel::validate,
        onPreviewConfigurationErrors = editFormViewModel::previewConfigurationErrors,
        onValidatePreviewAnswers = editFormViewModel::validatePreviewAnswers,
        onCanAdvanceToSaveShare = editFormViewModel::canAdvanceToSaveShare,
        onOpenDraft = editFormViewModel::openDraft,
        onRetrySave = editFormViewModel::retrySave,
        onRetryEditorLoad = editFormViewModel::retryLoad,
        onRetryFormsLoad = formsViewModel::retryLoading,
        onPublish = saveShareViewModel::publish, onArchive = saveShareViewModel::archive,
        saveShareUiState = saveShareUiState,
        onOpenSaveShare = saveShareViewModel::openDraft,
        onRetrySaveShareLoad = saveShareViewModel::retryLoading,
        onCopyLink = { copyFormLink(context, it) },
        onWhatsAppShare = { shareFormLinkOnWhatsApp(context, it) }
    )
}

@Composable
private fun FormLinkContent(
    guestImportNotice: @Composable () -> Unit = {},
    onPublish: (() -> Unit)? = null, onArchive: (() -> Unit)? = null,
    formsUiState: FormsUiState,
    chooseTemplateUiState: ChooseTemplateUiState,
    editFormUiState: EditFormUiState,
    onFilterSelected: (FormFilter) -> Unit,
    onStartEditor: (FormTemplate?) -> Unit,
    onBeginCreation: () -> Unit = {},
    onEditorAction: (EditFormAction) -> Unit,
    onValidateEditor: () -> FormValidationResult,
    onPreviewConfigurationErrors: () -> Map<String, Set<FieldError>>,
    onValidatePreviewAnswers: (Map<String, FormAnswer>, Boolean, Locale) -> Map<String, FieldError>,
    onCanAdvanceToSaveShare: () -> Boolean,
    onImportCover: (String) -> Unit = {},
    onRemoveCover: () -> Unit = {},
    onContinue: (() -> FormValidationResult)? = null,
    onConsumeContinue: () -> Unit = {},
    onOpenDraft: (String) -> Unit = {},
    onRetrySave: () -> Unit = {},
    onRetryEditorLoad: () -> Unit = {},
    onRetryFormsLoad: () -> Unit = {},
    saveShareUiState: SaveShareUiState = SaveShareUiState(),
    onOpenSaveShare: (String) -> Unit = {},
    onRetrySaveShareLoad: () -> Unit = {},
    onCopyLink: (String) -> Boolean = { false },
    onWhatsAppShare: (String) -> Boolean = { false },
    requestRepository: RequestRepository = UnavailableRequestRepository,
    requestsAreDemo: Boolean = false,
    onAccountClick: (() -> Unit)? = null,
    syncMessage: String? = null,
    dashboardState: ResponseDashboardUiState = ResponseDashboardUiState(),
    onDashboardForm: (String?) -> Unit = {},
    onDashboardPeriod: (ResponsePeriod) -> Unit = {},
    onDashboardRefresh: () -> Unit = {}
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
    var selectedRequestFormId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedResponseId by rememberSaveable { mutableStateOf<String?>(null) }
    val requestListState = rememberLazyListState()
    val dashboardListState = rememberLazyListState()
    var inboxStatus by rememberSaveable { mutableStateOf("ALL") }
    var detailReturnDestination by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD) }
    val showDashboard = { currentDestination = AppDestination.DASHBOARD }
    val openResponse: (com.uvg.cc3087.myapp.data.model.RequestSummary) -> Unit = { request ->
        detailReturnDestination = currentDestination
        selectedRequestFormId = request.formId
        selectedResponseId = request.responseId
        currentDestination = AppDestination.REQUEST_DETAIL
    }
    val openInbox: (String) -> Unit = { status ->
        inboxStatus = status
        requestListState.requestScrollToItem(0)
        currentDestination = AppDestination.REQUESTS
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
            AppDestination.DASHBOARD -> showForms()
            AppDestination.REQUESTS -> showDashboard()
            AppDestination.REQUEST_DETAIL -> { currentDestination = detailReturnDestination }
        }
    }

    when (currentDestination) {
        AppDestination.FORMS -> {
            Forms(
                guestImportNotice = guestImportNotice,
                onAccountClick = onAccountClick,
                syncMessage = syncMessage,
                onResponsesClick = showDashboard,
                hasUnreadResponses = dashboardState.dashboard.hasUnread,
                uiState = formsUiState,
                onFilterSelected = onFilterSelected,
                onNewFormClick = {
                    onBeginCreation()
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
                onResponsesClick = showDashboard,
                hasUnreadResponses = dashboardState.dashboard.hasUnread,
                uiState = chooseTemplateUiState,
                onBackClick = showForms,
                onBlankFormClick = { showEditor(null) },
                onTemplateClick = showEditor
            )
        }

        AppDestination.EDIT_FORM -> {
            EditForm(
                uiState = editFormUiState,
                syncMessage = syncMessage,
                onAction = onEditorAction,
                onImportCover = onImportCover,
                onRemoveCover = onRemoveCover,
                onContinue = onContinue,
                onConsumeContinue = onConsumeContinue,
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
                onPublish = onPublish, onArchive = onArchive,
                onCopyLink = onCopyLink,
                onWhatsAppShare = onWhatsAppShare
            )
        }

        AppDestination.DASHBOARD -> MyappTheme(dynamicColor = false) {
            ResponseDashboardScreen(dashboardState, dashboardListState,
                onForm = onDashboardForm, onPeriod = onDashboardPeriod,
                onInbox = { openInbox(it?.name ?: "ALL") }, onUnknown = { openInbox("UNKNOWN") },
                onOpen = openResponse, onBack = showForms, onRefresh = onDashboardRefresh,
                syncMessage = syncMessage, isDemo = requestsAreDemo)
        }

        AppDestination.REQUESTS -> MyappTheme(dynamicColor = false) {
            val data = dashboardState.dashboard
            val rows = data.requests.filter { request -> when (inboxStatus) {
                "ALL" -> true
                "UNKNOWN" -> request.requestStatus == null
                else -> request.requestStatus?.name == inboxStatus
            } }
            val status = if (dashboardState.hasData) {
                if (rows.isEmpty()) RequestListLoadStatus.EMPTY else RequestListLoadStatus.CONTENT
            } else dashboardState.status
            RequestListScreen(RequestListUiState(status, rows), openResponse, showDashboard, onDashboardRefresh,
                requestListState, requestsAreDemo, hasUnreadResponses = data.hasUnread,
                filters = {
                    ResponseFilters(dashboardState.filter, data.forms,
                        { onDashboardForm(it); requestListState.requestScrollToItem(0) },
                        { onDashboardPeriod(it); requestListState.requestScrollToItem(0) })
                    syncMessage?.let { androidx.compose.material3.Text(it) }
                    if (dashboardState.hasData && dashboardState.status != RequestListLoadStatus.CONTENT)
                        com.uvg.cc3087.myapp.ui.components.ResponseLoadMessage(dashboardState.status, onDashboardRefresh)
                },
                statusFilter = inboxStatus, onStatusFilter = { inboxStatus = it; requestListState.requestScrollToItem(0) },
                hasUnknownStatus = data.unknownStatusCount > 0, onForms = showForms)
        }

        AppDestination.REQUEST_DETAIL -> {
            RequestDetailRoute(
                formId = selectedRequestFormId.orEmpty(),
                responseId = selectedResponseId.orEmpty(),
                repository = requestRepository,
                onBack = { currentDestination = detailReturnDestination },
                isDemo = requestsAreDemo
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
