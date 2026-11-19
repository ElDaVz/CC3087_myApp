package com.uvg.cc3087.myapp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.repository.CoverStorage
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.RequestRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.data.repository.UnavailableRequestRepository
import com.uvg.cc3087.myapp.di.AppContainer
import com.uvg.cc3087.myapp.domain.telemetry.NoProductAnalytics
import com.uvg.cc3087.myapp.domain.telemetry.ProductAnalytics
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.state.FormsLoadStatus
import com.uvg.cc3087.myapp.ui.state.FormsUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.ChooseTemplateViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.EditFormViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.FormsViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.ResponseDashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun FormLinkApp() {
    val context = LocalContext.current.applicationContext
    val container = remember(context) { AppContainer.also { it.initialize(context) } }
    val resources = LocalResources.current
    val languageTags = resources.configuration.locales.toLanguageTags()
    LaunchedEffect(languageTags) {
        container.updateTemplateFields(FormEditorSampleData.templateFields(resources))
    }
    var initializationAttempt by remember { mutableIntStateOf(0) }
    val initialized = remember(initializationAttempt) { runCatching { container.backend } }
    if (initialized.isFailure) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.protected_data_error))
            Button(onClick = { initializationAttempt++ }) { Text(stringResource(R.string.retry)) }
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
    coverStorage: CoverStorage? = null,
    syncMessage: String? = null,
    analytics: ProductAnalytics = NoProductAnalytics,
    guestImportNotice: @Composable () -> Unit = {},
    accountContent: (@Composable (() -> Unit) -> Unit)? = null
) {
    // el editor y los filtros pertenecen a la sesión cifrada, no a una pantalla temporal
    val forms: FormsViewModel = viewModel(factory = FormsViewModel.factory(formRepository))
    val templates: ChooseTemplateViewModel = viewModel(factory = ChooseTemplateViewModel.factory(templateRepository))
    val editor: EditFormViewModel = viewModel(
        factory = EditFormViewModel.factory(templateRepository, formRepository, coverStorage, analytics)
    )
    val dashboard: ResponseDashboardViewModel = viewModel(factory = ResponseDashboardViewModel.factory(requestRepository))
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(dashboard, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                dashboard.checkCalendar()
                delay(30_000)
            }
        }
    }
    FormLinkGraph(forms, templates, editor, dashboard, formRepository, requestRepository,
        requestsAreDemo, syncMessage, guestImportNotice, accountContent)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FormLinkAppPreview() {
    MyappTheme(dynamicColor = false) {
        Forms(uiState = FormsUiState(forms = FormSampleData.forms, status = FormsLoadStatus.CONTENT))
    }
}
