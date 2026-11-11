package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.uvg.cc3087.myapp.R
import java.io.File

@Composable
fun FormBanner(title: String, coverPath: String?, modifier: Modifier = Modifier, remoteUrl: String? = null) {
    Box(modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)) {
        val remote = remoteUrl?.takeIf { com.uvg.cc3087.myapp.data.model.CoverAsset.allowedUrl(it) }
        if (coverPath != null || remote != null) {
            if (coverPath == null) AsyncImage(model = coil3.request.ImageRequest.Builder(LocalContext.current)
                .data(remote).diskCachePolicy(coil3.request.CachePolicy.DISABLED).build(), contentDescription = null,
                modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            if (coverPath != null) PrivateCoverImage(coverPath + ".blur.jpg", null, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .25f), Color.Black.copy(alpha = .7f)))))
        }
        Text(title.ifBlank { stringResource(R.string.untitled_form) },
            style = MaterialTheme.typography.headlineMedium,
            color = if (coverPath == null && remote == null) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp))
    }
}
