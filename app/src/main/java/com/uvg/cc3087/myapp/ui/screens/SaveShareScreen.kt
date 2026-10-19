package com.uvg.cc3087.myapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.ui.components.DraftLoadStatus
import com.uvg.cc3087.myapp.ui.components.SaveShareAsset
import com.uvg.cc3087.myapp.ui.state.SaveShareUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveShareScreen(
    uiState: SaveShareUiState,
    onBack: () -> Unit,
    onBackToForms: () -> Unit,
    onRetry: () -> Unit,
    onCopyLink: (String) -> Boolean,
    onWhatsAppShare: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun copyLink() {
        val link = uiState.publicLink?.takeIf { uiState.canShare } ?: return
        val message = if (onCopyLink(link)) R.string.link_copied else R.string.link_copy_error
        scope.launch { snackbar.showSnackbar(resources.getString(message)) }
    }
    fun shareLink() {
        val link = uiState.publicLink?.takeIf { uiState.canShare } ?: return
        if (!onWhatsAppShare(link)) {
            scope.launch { snackbar.showSnackbar(resources.getString(R.string.whatsapp_unavailable)) }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.save_and_share), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        val backDescription = stringResource(R.string.go_back)
                        IconButton(onClick = onBack, modifier = Modifier.semantics {
                            contentDescription = backDescription
                        }) {
                            SaveShareAsset(R.raw.save_share_back, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        if (uiState.isLoading || uiState.draft == null) {
            DraftLoadStatus(
                uiState.loadError, onRetry,
                Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            )
        } else BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        Modifier.size(96.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { SaveShareAsset(R.raw.save_share_success, Modifier.size(40.dp)) }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.form_saved),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(if (uiState.canShare) R.string.form_ready_to_share else R.string.form_saved_locally),
                        modifier = Modifier.widthIn(max = 280.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    ShareableLinkCard(uiState.publicLink, uiState.canShare, ::copyLink)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        enabled = uiState.canShare,
                        onClick = ::shareLink,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        SaveShareAsset(
                            R.raw.save_share_whatsapp, Modifier.size(20.dp),
                            if (uiState.canShare) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.share_via_whatsapp), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        enabled = uiState.canShare,
                        onClick = ::copyLink,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        SaveShareAsset(
                            R.raw.save_share_link, Modifier.size(width = 16.6667.dp, height = 8.33333.dp),
                            if (uiState.canShare) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.copy_link), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(24.dp))
                    SavedFormStats(uiState.draft.fields.size, uiState.canShare)
                }
                TextButton(onClick = onBackToForms, modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                    Text(stringResource(R.string.back_to_my_forms), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ShareableLinkCard(link: String?, canShare: Boolean, onCopy: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.shareable_link),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        link ?: stringResource(R.string.no_public_link),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val description = stringResource(R.string.copy_link)
                    IconButton(enabled = canShare, onClick = onCopy, modifier = Modifier.semantics {
                        contentDescription = description
                    }) {
                        SaveShareAsset(
                            R.raw.save_share_copy, Modifier.size(width = 14.1667.dp, height = 16.6667.dp),
                            if (canShare) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedFormStats(fieldCount: Int, published: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(48.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SavedFormStat(stringResource(R.string.saved_fields), fieldCount.toString(), Modifier.weight(1f))
            VerticalDivider(Modifier.height(32.dp))
            SavedFormStat(stringResource(R.string.responses), "0", Modifier.weight(1f))
            VerticalDivider(Modifier.height(32.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.saved_status), style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(
                        if (published) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        CircleShape
                    ))
                    Text(stringResource(if (published) R.string.active else R.string.draft), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SavedFormStat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}
