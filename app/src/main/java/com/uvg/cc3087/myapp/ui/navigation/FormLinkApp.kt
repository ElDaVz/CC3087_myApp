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
import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.di.AppContainer
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.resources.titleResId
import com.uvg.cc3087.myapp.ui.screens.ChooseTemplate
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.state.ChooseTemplateUiState
import com.uvg.cc3087.myapp.ui.state.EditFormAction
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.state.FormFilter
import com.uvg.cc3087.myapp.ui.state.FormsUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import com.uvg.cc3087.myapp.ui.viewmodel.ChooseTemplateViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.EditFormViewModel
import com.uvg.cc3087.myapp.ui.viewmodel.FormsViewModel

private enum class AppDestination {
    FORMS,
    CHOOSE_TEMPLATE,
    EDIT_FORM
}

@Composable
fun FormLinkApp() {
    val context = LocalContext.current
    // este viewmodel vive en la actividad y conserva el filtro al ir a plantillas :D
    val formsViewModel: FormsViewModel = viewModel(
        factory = FormsViewModel.factory(AppContainer.formRepository)
    )
    val formsUiState by formsViewModel.uiState.collectAsStateWithLifecycle()
    val chooseTemplateViewModel: ChooseTemplateViewModel = viewModel(
        factory = ChooseTemplateViewModel.factory(AppContainer.templateRepository)
    )
    val chooseTemplateUiState by chooseTemplateViewModel.uiState.collectAsStateWithLifecycle()
    val editFormViewModel: EditFormViewModel = viewModel(
        factory = EditFormViewModel.factory(AppContainer.templateRepository)
    )
    val editFormUiState by editFormViewModel.uiState.collectAsStateWithLifecycle()

    FormLinkContent(
        formsUiState = formsUiState,
        chooseTemplateUiState = chooseTemplateUiState,
        editFormUiState = editFormUiState,
        onFilterSelected = formsViewModel::selectFilter,
        onStartEditor = { template ->
            editFormViewModel.startDraft(
                // el idioma se resuelve en la ui y los campos los decide el viewmodel
                title = context.getString(template?.type?.titleResId ?: R.string.untitled_form),
                templateType = template?.type
            )
        },
        onEditorAction = editFormViewModel::onAction,
        onValidateEditor = editFormViewModel::validate
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
    onValidateEditor: () -> FormValidationResult
) {
    // cada pantalla recibe su estado y la navegación sigue aquí
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.FORMS)
    }

    val showForms = {
        currentDestination = AppDestination.FORMS
    }

    val showTemplates = {
        currentDestination = AppDestination.CHOOSE_TEMPLATE
    }

    val showEditor: (FormTemplate?) -> Unit = { template ->
        // solo iniciar desde plantillas reinicia el borrador, no una recomposición o rotación
        onStartEditor(template)
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
            chooseTemplateUiState = ChooseTemplateUiState(templates = TemplateSampleData.templates),
            editFormUiState = EditFormUiState(),
            onFilterSelected = {},
            onStartEditor = {},
            onEditorAction = {},
            onValidateEditor = { FormValidationResult.READY }
        )
    }
}
