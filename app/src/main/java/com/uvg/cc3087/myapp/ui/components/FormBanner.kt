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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R

@Composable
fun FormBanner(title: String, coverPath: String?, modifier: Modifier = Modifier, remoteUrl: String? = null, coverAction: String = "KEEP") {
    FormCoverImage(coverPath, remoteUrl, coverAction, null,
        modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)), blurred = true) { hasImage ->
        if (hasImage) Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .25f), Color.Black.copy(alpha = .7f)))))
        else Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primaryContainer))
        Text(title.ifBlank { stringResource(R.string.untitled_form) },
            style = MaterialTheme.typography.headlineMedium,
            color = if (hasImage) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp))
    }
}
