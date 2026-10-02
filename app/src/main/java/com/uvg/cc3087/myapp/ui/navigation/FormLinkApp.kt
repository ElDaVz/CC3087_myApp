    package com.uvg.cc3087.myapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.ui.screens.ChooseTemplate
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.FormFilter
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.theme.MyappTheme

private enum class AppDestination {
    FORMS,
    CHOOSE_TEMPLATE,
    EDIT_FORM
}

@Composable
fun FormLinkApp() {
    val context = LocalContext.current
    // este estado pequeño es suficiente para conectar las dos pantallas sin otra dependencia :D
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.FORMS)
    }

    // guardamos el filtro aquí para que no se pierda al volver de plantillas :D
    var selectedFilter by rememberSaveable {
        mutableStateOf(FormFilter.ALL)
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
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
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
        FormLinkApp()
    }
}
