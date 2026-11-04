package com.uvg.cc3087.myapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.ui.screens.RequestDetailScreen
import com.uvg.cc3087.myapp.ui.viewmodel.RequestDetailViewModel
import com.uvg.cc3087.myapp.ui.state.RequestDetailLoadStatus
import com.uvg.cc3087.myapp.ui.state.RequestDetailUiState

/** Entry point shared by the response list and the debug demo. */
@Composable
fun RequestDetailRoute(
    formId: String,
    responseId: String,
    repository: RequestRepository,
    onBack: () -> Unit,
    isDemo: Boolean = false
) {
    val model: RequestDetailViewModel = viewModel(factory = RequestDetailViewModel.factory(repository))
    val state by model.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(formId, responseId) { model.openRequest(formId, responseId, refresh = true) }
    BackHandler(onBack = onBack)
    // Do not render the previous selection while the new load effect is being scheduled.
    val visibleState = if (state.detail?.let { it.formId != formId || it.responseId != responseId } == true)
        RequestDetailUiState(RequestDetailLoadStatus.LOADING) else state
    RequestDetailScreen(visibleState, onBack, model::retryLoading, isDemo = isDemo, onStatusChange = model::changeStatus)
}
