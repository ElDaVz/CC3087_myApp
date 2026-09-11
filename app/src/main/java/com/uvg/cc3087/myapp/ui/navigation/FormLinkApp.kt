package com.uvg.cc3087.myapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.uvg.cc3087.myapp.ui.screens.ChooseTemplate
import com.uvg.cc3087.myapp.ui.screens.FormFilter
import com.uvg.cc3087.myapp.ui.screens.Forms
import com.uvg.cc3087.myapp.ui.theme.MyappTheme

private enum class AppDestination {
    FORMS,
    CHOOSE_TEMPLATE
}

@Composable
fun FormLinkApp() {
    // este estado pequeño es suficiente para conectar las dos pantallas sin otra dependencia :D
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.FORMS)
    }

    // guardamos el filtro aquí para que no se pierda al volver de plantillas :D
    var selectedFilter by rememberSaveable {
        mutableStateOf(FormFilter.ALL)
    }

    val showForms = {
        currentDestination = AppDestination.FORMS
    }

    // el botón físico de regreso también vuelve a forms desde choose template
    BackHandler(enabled = currentDestination == AppDestination.CHOOSE_TEMPLATE) {
        showForms()
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
            ChooseTemplate(onBackClick = showForms)
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
