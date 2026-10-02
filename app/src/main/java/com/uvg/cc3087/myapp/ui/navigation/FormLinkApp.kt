package com.uvg.cc3087.myapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.local.FullFormDraft
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.repository.LocalFormRepository
import com.uvg.cc3087.myapp.ui.screens.ChooseTemplate
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.FormFilter
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import kotlinx.coroutines.launch

private enum class AppDestination {
    FORMS,
    CHOOSE_TEMPLATE,
    EDIT_FORM
}

@Composable
fun FormLinkApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { LocalFormRepository(context) }

    val forms by repository.observeForms().collectAsState(initial = emptyList())

    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.FORMS)
    }

    var selectedFilter by rememberSaveable {
        mutableStateOf(FormFilter.ALL)
    }

    var selectedFormId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedTemplateTitle by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var currentFormFields by remember {
        mutableStateOf<List<FormFieldDraft>>(emptyList())
    }

    val showForms = {
        currentDestination = AppDestination.FORMS
    }

    val showTemplates = {
        currentDestination = AppDestination.CHOOSE_TEMPLATE
    }

    val openFormEditor: (String, String, List<FormFieldDraft>) -> Unit = { formId, title, fields ->
        selectedFormId = formId
        selectedTemplateTitle = title
        currentFormFields = fields
        coroutineScope.launch {
            repository.setLastEditedFormId(formId)
        }
        currentDestination = AppDestination.EDIT_FORM
    }

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
                forms = forms,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                onNewFormClick = {
                    currentDestination = AppDestination.CHOOSE_TEMPLATE
                },
                onFormClick = { summary ->
                    coroutineScope.launch {
                        val draft = repository.getForm(summary.id)
                        openFormEditor(
                            summary.id,
                            summary.title,
                            draft?.fields ?: FormEditorSampleData.templateFields
                        )
                    }
                }
            )
        }

        AppDestination.CHOOSE_TEMPLATE -> {
            ChooseTemplate(
                onBackClick = showForms,
                onBlankFormClick = {
                    val newId = java.util.UUID.randomUUID().toString()
                    openFormEditor(newId, context.getString(R.string.untitled_form), emptyList())
                },
                onTemplateClick = { template ->
                    val newId = java.util.UUID.randomUUID().toString()
                    val title = context.getString(template.titleResId)
                    openFormEditor(newId, title, FormEditorSampleData.templateFields)
                }
            )
        }

        AppDestination.EDIT_FORM -> {
            val activeFormId = selectedFormId ?: remember { java.util.UUID.randomUUID().toString() }
            EditForm(
                formId = activeFormId,
                initialTitle = selectedTemplateTitle ?: context.getString(R.string.untitled_form),
                initialFields = currentFormFields,
                onBackClick = showForms,
                onSaveForm = { draft ->
                    coroutineScope.launch {
                        repository.saveForm(draft)
                    }
                }
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
