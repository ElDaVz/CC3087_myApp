package com.uvg.cc3087.myapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Encrypted private logo. An unavailable file renders nothing, never a broken asset. */
@Composable
fun BusinessLogoImage(path: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            if (path == null) return@withContext null
            try {
                check(path.startsWith("logos/") && !path.contains(".."))
                val plain = DeviceCipher(context).decrypt(File(context.filesDir, path).readBytes())
                BitmapFactory.decodeByteArray(plain, 0, plain.size)?.asImageBitmap()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }
    }
    val image = bitmap
    if (image != null) Image(
        image, contentDescription,
        modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}
