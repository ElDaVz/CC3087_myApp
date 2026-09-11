package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.ui.components.FormListItem
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import kotlinx.coroutines.launch

// estas opciones describen los filtros disponibles para la pantalla
enum class FormFilter(val label: String) {
    ALL("Todos"),
    ACTIVE("Activos"),
    DRAFT("Borradores")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Forms(
    modifier: Modifier = Modifier,
    forms: List<FormSummary> = FormSampleData.forms,
    selectedFilter: FormFilter = FormFilter.ALL,
    onFilterSelected: (FormFilter) -> Unit = {},
    onNewFormClick: () -> Unit = {},
    onFormClick: (FormSummary) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // mantenemos la lista original intacta y solo cambiamos lo que se muestra
    val visibleForms = when (selectedFilter) {
        FormFilter.ALL -> forms
        FormFilter.ACTIVE -> forms.filter { it.status == FormStatus.ACTIVE }
        FormFilter.DRAFT -> forms.filter { it.status == FormStatus.DRAFT }
    }

    // el snackbar hace visible cada interacción mientras conectamos la navegación
    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "FormLink") },
                actions = {
                    IconButton(onClick = { showMessage("Buscar formularios") }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Buscar formularios"
                        )
                    }
                    IconButton(onClick = { showMessage("Más opciones") }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Más opciones"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            FormsBottomBar(onMessage = ::showMessage)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "forms-controls") {
                FormsControls(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                    onNewFormClick = {
                        onNewFormClick()
                        showMessage("Nuevo formulario")
                    }
                )
            }

            // usamos el id real para que compose identifique cada elemento correctamente
            items(
                items = visibleForms,
                key = { form -> form.id }
            ) { form ->
                FormListItem(
                    form = form,
                    onClick = {
                        onFormClick(form)
                        showMessage("${form.title} seleccionado")
                    },
                    onShareClick = { showMessage("Compartir: ${form.title}") },
                    onEditClick = { showMessage("Editar: ${form.title}") },
                    onViewClick = { showMessage("Ver: ${form.title}") }
                )
            }
        }
    }
}

// dejamos los controles separados para que la pantalla principal sea fácil de leer
@Composable
private fun FormsControls(
    selectedFilter: FormFilter,
    onFilterSelected: (FormFilter) -> Unit,
    onNewFormClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis formularios",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = onNewFormClick,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Nuevo formulario")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(text = filter.label) }
                )
            }
        }
    }
}

// la barra inferior también queda aislada para poder ajustarla sin tocar la lista
@Composable
private fun FormsBottomBar(onMessage: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { onMessage("Formularios seleccionados") },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null
                )
            },
            label = { Text(text = "Formularios") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { onMessage("Respuestas seleccionadas") },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null
                )
            },
            label = { Text(text = "Respuestas") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { onMessage("Ajustes seleccionados") },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null
                )
            },
            label = { Text(text = "Ajustes") }
        )
    }
}

@Preview(
    name = "Forms screen",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun FormsPreview() {
    MyappTheme(dynamicColor = false) {
        Forms()
    }
}
