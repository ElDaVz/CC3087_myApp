package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.FormFieldType

private data class FieldPaletteItem(
    val type: FormFieldType,
    @param:StringRes val descriptionRes: Int,
    val icon: ImageVector
)

private val availableFieldTypes = listOf(
    FieldPaletteItem(
        type = FormFieldType.TEXT,
        descriptionRes = R.string.field_text_description,
        icon = Icons.Outlined.TextFields
    ),
    FieldPaletteItem(
        type = FormFieldType.MULTIPLE_CHOICE,
        descriptionRes = R.string.field_selection_description,
        icon = Icons.Outlined.RadioButtonChecked
    ),
    FieldPaletteItem(
        type = FormFieldType.DATE,
        descriptionRes = R.string.field_date_description,
        icon = Icons.Outlined.CalendarToday
    ),
    FieldPaletteItem(FormFieldType.PARAGRAPH, R.string.field_paragraph_description, Icons.Outlined.TextFields),
    FieldPaletteItem(FormFieldType.NUMBER, R.string.field_number_description, Icons.Outlined.TextFields),
    FieldPaletteItem(FormFieldType.EMAIL, R.string.field_email_description, Icons.Outlined.TextFields),
    FieldPaletteItem(FormFieldType.CHECKBOX, R.string.field_confirmation_description, Icons.Outlined.RadioButtonChecked),
    FieldPaletteItem(FormFieldType.DROPDOWN, R.string.field_dropdown_description, Icons.Outlined.RadioButtonChecked)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFieldBottomSheet(
    onDismissRequest: () -> Unit,
    onFieldSelected: (FormFieldType) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val resources = LocalResources.current
    val visibleTypes = availableFieldTypes.filter { item ->
        resources.getString(item.type.labelRes).contains(searchQuery, ignoreCase = true) ||
            resources.getString(item.descriptionRes).contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.add_field),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.done))
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.search_field_type)) }
            )

            Text(
                text = stringResource(R.string.available_fields),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (visibleTypes.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_matching_fields),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 320.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleTypes, key = { item -> item.type.name }) { item ->
                        FieldPaletteCard(
                            item = item,
                            onClick = { onFieldSelected(item.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldPaletteCard(
    item: FieldPaletteItem,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(item.type.labelRes),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(item.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
