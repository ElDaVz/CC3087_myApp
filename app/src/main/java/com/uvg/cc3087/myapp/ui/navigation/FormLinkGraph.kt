package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.ui.resources.copyFormLink
import com.uvg.cc3087.myapp.ui.resources.shareFormLinkOnWhatsApp
import com.uvg.cc3087.myapp.ui.resources.titleResId
import com.uvg.cc3087.myapp.ui.screens.ChooseTemplate
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.screens.ResponseDashboardScreen
import com.uvg.cc3087.myapp.ui.screens.SaveShareScreen
import com.uvg.cc3087.myapp.ui.screens.SettingsScreen
import com.uvg.cc3087.myapp.ui.theme.LocalAppearance
import com.uvg.cc3087.myapp.ui.state.DraftSaveStatus
import com.uvg.cc3087.myapp.ui.viewmodel.ChooseTemplateViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.EditFormViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.FormsViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.FieldAnalyticsViewModel
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.uvg.cc3087.myapp.ui.viewmodel.SaveShareViewModel

@Composable
internal fun FormLinkGraph(
    forms: FormsViewModel,
    templates: ChooseTemplateViewModel,
    editor: EditFormViewModel,
    dashboard: ResponseDashboardViewModel,
    formRepository: FormRepository,
    requestRepository: RequestRepository,
    requestsAreDemo: Boolean,
    syncMessage: String?,
    guestImportNotice: @Composable () -> Unit,
    accountContent: (@Composable (() -> Unit) -> Unit)?
) {
    val backStack = rememberNavBackStack(AppRoute.Forms)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val resources = LocalResources.current
    val context = LocalContext.current
    // también conservamos el desplazamiento al cambiar de pestaña
    val dashboardList = rememberLazyListState()
    val requestList = rememberLazyListState()
    val statisticsList = rememberLazyListState()
    val startEditor: (FormTemplate?) -> Unit = { template ->
        // dos toques rápidos no deben crear dos borradores durante la transición
        if (navigator.isCurrent(AppRoute.ChooseTemplate)) {
            editor.startDraft(
                resources.getString(template?.type?.titleResId ?: R.string.untitled_form),
                template?.type
            )
            navigator.navigate(AppRoute.Editor(checkNotNull(editor.uiState.value.draftId)))
        }
    }

    @Composable
    fun DashboardDestination(route: AppRoute) {
        val state by dashboard.uiState.collectAsStateWithLifecycle()
        val fieldAnalytics: FieldAnalyticsViewModel = viewModel(factory = FieldAnalyticsViewModel.factory(requestRepository))
        val fieldState by fieldAnalytics.uiState.collectAsStateWithLifecycle()
        val lifecycle = LocalLifecycleOwner.current
        LaunchedEffect(state.filter, state.hasData, state.status) {
            fieldAnalytics.select(state.filter.formId.takeIf {
                state.hasData && state.status != RequestListLoadStatus.FORBIDDEN
            }, state.filter.period)
        }
        LaunchedEffect(fieldAnalytics, lifecycle) {
            lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) { fieldAnalytics.checkCalendar(); delay(30_000) }
            }
        }
        ResponseDashboardScreen(
            state = state,
            listState = if (route == AppRoute.Statistics) statisticsList else dashboardList,
            onForm = dashboard::selectForm,
            onPeriod = dashboard::selectPeriod,
            onInbox = {
                requestList.requestScrollToItem(0)
                navigator.navigate(AppRoute.Responses(InboxFilter.fromKey(it?.name ?: "ALL")))
            },
            onUnknown = {
                requestList.requestScrollToItem(0)
                navigator.navigate(AppRoute.Responses(InboxFilter.UNKNOWN))
            },
            onOpen = { navigator.navigate(AppRoute.RequestDetail(it.formId, it.responseId)) },
            onBack = { navigator.backFrom(route) },
            onForms = navigator::showForms,
            onSettingsClick = navigator::showSettings,
            onRefresh = { dashboard.refresh(); fieldAnalytics.retry() },
            syncMessage = syncMessage,
            isDemo = requestsAreDemo,
            analyticsState = fieldState,
            onAnalyticsRetry = fieldAnalytics::retry
        )
    }

    NavDisplay(
        backStack = backStack,
        onBack = navigator::back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<AppRoute.Forms> {
                val state by forms.uiState.collectAsStateWithLifecycle()
                val editorState by editor.uiState.collectAsStateWithLifecycle()
                val dashboardState by dashboard.uiState.collectAsStateWithLifecycle()
                Forms(
                    uiState = state,
                    onFilterSelected = forms::selectFilter,
                    guestImportNotice = guestImportNotice,
                    syncMessage = syncMessage,
                    onAccountClick = if (accountContent != null) {
                        { navigator.navigate(AppRoute.Account) }
                    } else null,
                    onResponsesClick = navigator::showDashboard,
                    onSettingsClick = navigator::showSettings,
                    hasUnreadResponses = dashboardState.dashboard.hasUnread,
                    onNewFormClick = {
                        if (navigator.isCurrent(AppRoute.Forms)) {
                            editor.beginCreationAttempt()
                            navigator.navigate(AppRoute.ChooseTemplate)
                        }
                    },
                    onFormClick = { form ->
                        if (navigator.isCurrent(AppRoute.Forms)) {
                            editor.openDraft(form.id)
                            navigator.navigate(AppRoute.Editor(form.id))
                        }
                    },
                    onRetryLoad = forms::retryLoading,
                    onDeleteFormClick = forms::requestDeletion,
                    onConfirmDeletion = forms::confirmDeletion,
                    onDismissDeletion = forms::dismissDeletion,
                    onConsumeDeletionCompleted = forms::consumeDeletionCompleted,
                    unsavedDraftId = editorState.takeIf {
                        it.saveStatus != DraftSaveStatus.SAVED
                    }?.draftId,
                    onShareFormClick = { navigator.navigate(AppRoute.SaveShare(it.id)) }
                )
            }
            entry<AppRoute.ChooseTemplate> { route ->
                val state by templates.uiState.collectAsStateWithLifecycle()
                val responses by dashboard.uiState.collectAsStateWithLifecycle()
                ChooseTemplate(
                    uiState = state,
                    onBackClick = { navigator.backFrom(route) },
                    onResponsesClick = navigator::showDashboard,
                    onSettingsClick = navigator::showSettings,
                    hasUnreadResponses = responses.dashboard.hasUnread,
                    onBlankFormClick = { startEditor(null) },
                    onTemplateClick = startEditor
                )
            }
            entry<AppRoute.Editor> { route ->
                FormEditorRoute(editor, route.draftId, navigator, syncMessage)
            }
            entry<AppRoute.Preview> { route ->
                FormEditorRoute(editor, route.draftId, navigator, syncMessage, preview = true)
            }
            entry<AppRoute.SaveShare> { route ->
                // este viewmodel se libera al sacar su comprobante de la pila
                val model: SaveShareViewModel = viewModel(
                    factory = SaveShareViewModel.factory(formRepository)
                )
                val state by model.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(route.draftId) { model.openDraft(route.draftId) }
                SaveShareScreen(
                    uiState = state,
                    onBack = { navigator.backFrom(route) },
                    onBackToForms = navigator::showForms,
                    onRetry = model::retryLoading,
                    onPublish = model::publish,
                    onArchive = model::archive,
                    onCopyLink = { copyFormLink(context, it) },
                    onWhatsAppShare = { shareFormLinkOnWhatsApp(context, it) }
                )
            }
            entry<AppRoute.Dashboard> { DashboardDestination(AppRoute.Dashboard) }
            entry<AppRoute.Statistics> { DashboardDestination(AppRoute.Statistics) }
            entry<AppRoute.Responses> { route ->
                var status by rememberSaveable { mutableStateOf(route.filter) }
                val state by dashboard.uiState.collectAsStateWithLifecycle()
                ResponsesRoute(
                    state = state,
                    status = status,
                    list = requestList,
                    onForm = dashboard::selectForm,
                    onPeriod = dashboard::selectPeriod,
                    onStatus = {
                        status = it
                        requestList.requestScrollToItem(0)
                    },
                    onOpen = { navigator.navigate(AppRoute.RequestDetail(it.formId, it.responseId)) },
                    onBack = { navigator.backFrom(route) },
                    onForms = navigator::showForms,
                    onRefresh = dashboard::refresh,
                    syncMessage = syncMessage,
                    isDemo = requestsAreDemo,
                    onSettings = navigator::showSettings
                )
            }
            entry<AppRoute.RequestDetail> { route ->
                RequestDetailRoute(
                    formId = route.formId,
                    responseId = route.responseId,
                    repository = requestRepository,
                    onBack = { navigator.backFrom(route) },
                    isDemo = requestsAreDemo
                )
            }
            entry<AppRoute.Settings> { route ->
                val appearance = LocalAppearance.current
                SettingsScreen(
                    appearance = appearance.state,
                    onTheme = appearance.select,
                    onRetry = appearance.retry,
                    onBack = { navigator.backFrom(route) },
                    onAccount = if (accountContent != null) {
                        { navigator.navigate(AppRoute.Account) }
                    } else null,
                    onStatistics = { navigator.navigate(AppRoute.Statistics) }
                )
            }
            entry<AppRoute.Account> { route ->
                accountContent?.invoke { navigator.backFrom(route) }
            }
        }
    )
}
