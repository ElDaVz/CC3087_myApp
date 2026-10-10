package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.ui.components.TemplateCard
import com.uvg.cc3087.myapp.ui.resources.subtitleResId
import com.uvg.cc3087.myapp.ui.resources.titleResId
import com.uvg.cc3087.myapp.ui.state.ChooseTemplateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseTemplate(
    uiState: ChooseTemplateUiState,
    onBackClick: () -> Unit = {},
    onBlankFormClick: () -> Unit = {},
    onTemplateClick: (FormTemplate) -> Unit = {}
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(stringResource(R.string.new_form))
                },

                navigationIcon = {

                    // esta acción la decide el host para no acoplar la pantalla a la navegación
                    IconButton(onClick = onBackClick) {

                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },

                actions = {

                    IconButton(onClick = {}) {

                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = null
                        )
                    }
                }
            )
        },

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = true,
                    onClick = onBackClick,
                    icon = { Icon(Icons.Outlined.AccountBox, null) },
                    label = { Text(stringResource(R.string.forms)) }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.AutoMirrored.Outlined.List, null) },
                    label = { Text(stringResource(R.string.responses)) }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text(stringResource(R.string.settings)) }
                )
            }
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()

        ) {

            Text(
                stringResource(R.string.start_from_scratch),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                onClick = onBlankFormClick
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        FilledIconButton(onClick = onBlankFormClick) {

                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(stringResource(R.string.blank_form))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.recommended_templates),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                items(
                    items = uiState.templates,
                    key = { template -> template.id }
                ) { template ->

                    TemplateCard(
                        title = stringResource(template.type.titleResId),
                        subtitle = stringResource(template.type.subtitleResId),
                        onClick = { onTemplateClick(template) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChooseTemplatePreview() {
    MaterialTheme {
        ChooseTemplate(uiState = ChooseTemplateUiState(templates = TemplateSampleData.templates))
    }
}
