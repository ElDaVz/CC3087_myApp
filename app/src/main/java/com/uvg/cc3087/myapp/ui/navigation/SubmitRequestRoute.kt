package com.uvg.cc3087.myapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.ui.screens.FormPreviewScreen
import com.uvg.cc3087.myapp.ui.viewmodel.SubmitRequestViewModel

// Isolated submission surface for fixtures; normal app navigation cannot reach it.
@Composable
internal fun SubmitRequestRoute(formId: String, responseId: String, forms: FormRepository,
    requests: RequestRepository, onBack: () -> Unit, onViewRequest: () -> Unit) {
    val model: SubmitRequestViewModel = viewModel(key = "submission-$responseId",
        factory = SubmitRequestViewModel.factory(forms, requests, formId, responseId))
    val state by model.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    val form = state.form
    if (!state.loading && !state.loadFailed && !state.saved && form != null) {
        FormPreviewScreen(title = form.title, fields = form.fields, onClose = onBack,
            validationResult = model.validationResult(),
            configurationErrors = model.configurationErrors(),
            validateAnswers = model::validateAnswers,
            onSubmitLocal = model::submit, submitting = state.saving, submitFailed = state.saveFailed)
    } else {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.go_back)) }
                when {
                    state.loading -> CircularProgressIndicator()
                    state.saved -> {
                        Text(stringResource(R.string.request_saved_local))
                        Button(onClick = onViewRequest) { Text(stringResource(R.string.request_view_saved)) }
                    }
                    else -> {
                        Text(stringResource(R.string.request_form_load_error))
                        Button(onClick = model::load) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
        }
    }
}
