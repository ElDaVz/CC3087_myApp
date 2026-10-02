package com.uvg.cc3087.myapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.ui.theme.MyappTheme

// los eventos llegan como callbacks para poder reutilizar la tarjeta en otras pantallas
@Composable
fun FormListItem(
    form: FormSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onViewClick: () -> Unit = {}
) {
    val responseText = pluralStringResource(
        R.plurals.response_count,
        form.responseCount,
        form.responseCount
    )

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val placeholderColor = MaterialTheme.colorScheme.surfaceVariant

            // coil carga la miniatura y deja un color suave mientras espera
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(form.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.thumbnail_for_form, form.title),
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = ColorPainter(placeholderColor),
                error = ColorPainter(placeholderColor),
                fallback = ColorPainter(placeholderColor),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = form.title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.form_response_summary, responseText, form.updatedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                StatusLabel(status = form.status)
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FormActionButton(
                label = stringResource(R.string.share),
                onClick = onShareClick,
                icon = Icons.Outlined.Share
            )
            FormActionButton(
                label = stringResource(R.string.edit),
                onClick = onEditClick,
                icon = Icons.Outlined.Edit
            )
            FormActionButton(
                label = stringResource(R.string.view),
                onClick = onViewClick,
                icon = Icons.Outlined.Visibility
            )
        }
    }
}

@Composable
private fun StatusLabel(status: FormStatus) {
    val isActive = status == FormStatus.ACTIVE

    // el color permite reconocer el estado sin agregar lógica visual a la pantalla
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = stringResource(if (isActive) R.string.active else R.string.draft),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun FormActionButton(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector
) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label)
    }
}

private val previewForm = FormSummary(
    id = "bakery-custom-order",
    title = "Pedido personalizado de pastelería",
    responseCount = 12,
    updatedDate = "18 jul",
    status = FormStatus.ACTIVE,
    imageUrl = "https://picsum.photos/seed/bakery-custom-order/160/160"
)

// estas previews nos ayudan a revisar la tarjeta en modo claro y oscuro :D
@Preview(name = "Form item - Light", showBackground = true)
@Preview(
    name = "Form item - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun FormListItemPreview() {
    MyappTheme(dynamicColor = false) {
        FormListItem(
            form = previewForm,
            modifier = Modifier.padding(16.dp)
        )
    }
}
