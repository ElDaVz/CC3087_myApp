package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Decode in memory; never allow an image loader's disk cache to persist plaintext. */
@Composable
fun PrivateCoverImage(path: String, description: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { runCatching {
            require(path.startsWith("covers/") && !path.contains(".."))
            val bytes = DeviceCipher(context).decrypt(File(context.filesDir, path).readBytes())
            requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()
        }.getOrNull() }
    }
    bitmap?.let { Image(it, description, modifier, contentScale = ContentScale.Crop) }
}
