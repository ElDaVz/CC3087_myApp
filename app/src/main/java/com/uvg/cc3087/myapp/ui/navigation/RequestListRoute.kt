package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.data.model.RequestSummary
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.ui.screens.RequestListScreen
import com.uvg.cc3087.myapp.ui.viewmodel.RequestListViewModel

@Composable
fun RequestListRoute(
    repository: RequestRepository,
    onOpenRequest: (RequestSummary) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState,
    isDemo: Boolean = false
) {
    val model: RequestListViewModel = viewModel(factory = RequestListViewModel.factory(repository))
    val state by model.uiState.collectAsStateWithLifecycle()
    RequestListScreen(state, onOpenRequest, onBack, model::refresh, listState, isDemo)
}
