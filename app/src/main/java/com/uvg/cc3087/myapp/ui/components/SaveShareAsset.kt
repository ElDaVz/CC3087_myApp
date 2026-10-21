package com.uvg.cc3087.myapp.ui.components

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage

@Composable
internal fun SaveShareAsset(@RawRes resourceId: Int, modifier: Modifier, tint: Color? = null) {
    val packageName = LocalContext.current.packageName
    // los svg vienen de figma y se cargan desde el apk, sin una petición de red
    AsyncImage(
        model = "android.resource://$packageName/$resourceId",
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}
