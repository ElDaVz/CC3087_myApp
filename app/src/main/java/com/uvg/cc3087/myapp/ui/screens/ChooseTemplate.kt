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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.ui.components.TemplateCard

data class Template(
    val title: String,
    val subtitle: String
)

private val templates = listOf(
    Template("Job Application", "Standard candidate intake"),
    Template("Order Form", "Product requests"),
    Template("Event RSVP", "Manage attendees"),
    Template("Feedback", "Customer surveys")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseTemplate(
    onBackClick: () -> Unit = {}
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("New Form")
                },

                navigationIcon = {

                    // esta acción la decide el host para no acoplar la pantalla a la navegación
                    IconButton(onClick = onBackClick) {

                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Go back"
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
                    label = { Text("Forms") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.AutoMirrored.Outlined.List, null) },
                    label = { Text("Responses") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text("Settings") }
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
                "Start from Scratch",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                onClick = {}
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        FilledIconButton(onClick = {}) {

                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text("Blank Form")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Recommended Templates",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                items(templates) {

                    TemplateCard(
                        title = it.title,
                        subtitle = it.subtitle
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
        ChooseTemplate()
    }
}
