package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.FormPreviewScreen
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.viewmodel.EditFormViewModel

@Composable
internal fun FormEditorRoute(
    model: EditFormViewModel,
    draftId: String,
    navigator: AppNavigator,
    syncMessage: String?,
    preview: Boolean = false
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val route = if (preview) AppRoute.Preview(draftId) else AppRoute.Editor(draftId)
    LaunchedEffect(draftId) {
        if (model.uiState.value.draftId != draftId) model.openDraft(draftId)
    }
    // una ruta restaurada no muestra contenido del borrador abierto anteriormente
    val visible = if (state.draftId == draftId) state else {
        EditFormUiState(draftId = draftId, isLoading = true)
    }
    val saveForm = {
        if (navigator.isCurrent(route) && model.uiState.value.draftId == draftId &&
            model.canAdvanceToSaveShare()
        ) navigator.showSaveShare(draftId)
    }
    if (preview && !visible.isLoading && visible.loadError == null) {
        FormPreviewScreen(
            title = visible.title,
            fields = visible.fields,
            onClose = { navigator.backFrom(route) },
            validationResult = model.validate(),
            configurationErrors = model.previewConfigurationErrors(),
            validateAnswers = model::validatePreviewAnswers,
            coverPath = visible.coverPath,
            publishedCoverUrl = visible.publishedCoverUrl,
            coverAction = visible.coverAction,
            saveStatus = visible.saveStatus,
            onRetrySave = model::retrySave,
            onSaveForm = saveForm,
            canSaveForm = model.canAdvanceToSaveShare()
        )
    } else {
        EditForm(
            uiState = visible,
            onAction = model::onAction,
            onValidate = model::validate,
            onBackClick = { navigator.backFrom(route) },
            onRetrySave = model::retrySave,
            onRetryLoad = model::retryLoad,
            configurationErrors = model::previewConfigurationErrors,
            validatePreviewAnswers = model::validatePreviewAnswers,
            canAdvanceToSaveShare = model::canAdvanceToSaveShare,
            onImportCover = model::importCover,
            onRemoveCover = model::removeCover,
            onContinue = model::requestContinue,
            onConsumeContinue = model::consumeContinue,
            onSaveForm = saveForm,
            syncMessage = syncMessage,
            onPreviewClick = {
                if (navigator.isCurrent(route)) navigator.navigate(AppRoute.Preview(draftId))
            }
        )
    }
}
