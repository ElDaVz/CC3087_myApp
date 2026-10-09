package com.uvg.cc3087.myapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.di.AppContainer
import com.uvg.cc3087.myapp.ui.screens.ChooseTemplate
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.state.FormFilter
import com.uvg.cc3087.myapp.ui.state.FormsUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.FormsViewModel

private enum class AppDestination {
    FORMS,
    CHOOSE_TEMPLATE,
    EDIT_FORM
}

@Composable
fun FormLinkApp() {
    // este viewmodel vive en la actividad y conserva el filtro al ir a plantillas :D
    val formsViewModel: FormsViewModel = viewModel(
        factory = FormsViewModel.factory(AppContainer.formRepository)
    )
    val formsUiState by formsViewModel.uiState.collectAsStateWithLifecycle()

    FormLinkContent(
        formsUiState = formsUiState,
        onFilterSelected = formsViewModel::selectFilter
    )
}

@Composable
private fun FormLinkContent(
    formsUiState: FormsUiState,
    onFilterSelected: (FormFilter) -> Unit
) {
    val context = LocalContext.current
    // la navegación queda en la ui y el estado de formularios queda en su viewmodel
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.FORMS)
    }

    var selectedTemplateTitle by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val showForms = {
        currentDestination = AppDestination.FORMS
    }

    val showTemplates = {
        currentDestination = AppDestination.CHOOSE_TEMPLATE
    }

    val showEditor: (String?) -> Unit = { templateTitle ->
        selectedTemplateTitle = templateTitle
        currentDestination = AppDestination.EDIT_FORM
    }

    // el botón físico sigue el mismo recorrido que las flechas de la app
    BackHandler(enabled = currentDestination != AppDestination.FORMS) {
        when (currentDestination) {
            AppDestination.CHOOSE_TEMPLATE -> showForms()
            AppDestination.EDIT_FORM -> showTemplates()
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
                }
            )
        }

        AppDestination.CHOOSE_TEMPLATE -> {
            ChooseTemplate(
                onBackClick = showForms,
                onBlankFormClick = { showEditor(null) },
                onTemplateClick = { template -> showEditor(context.getString(template.titleResId)) }
            )
        }

        AppDestination.EDIT_FORM -> {
            EditForm(
                initialTitle = selectedTemplateTitle ?: context.getString(R.string.untitled_form),
                initialFields = if (selectedTemplateTitle == null) {
                    emptyList()
                } else {
                    FormEditorSampleData.templateFields
                },
                onBackClick = showTemplates
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
            formsUiState = FormsUiState(forms = FormSampleData.forms),
            onFilterSelected = {}
        )
    }
}
