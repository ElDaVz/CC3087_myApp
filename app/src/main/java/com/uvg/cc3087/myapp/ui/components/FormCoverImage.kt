package com.uvg.cc3087.myapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.uvg.cc3087.myapp.data.model.CoverAsset
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A missing or undecodable private file is unavailable, even if Room still has its path. */
@Composable
fun FormCoverImage(
    path: String?,
    remoteUrl: String?,
    coverAction: String,
    description: String?,
    modifier: Modifier = Modifier,
    blurred: Boolean = false,
    content: @Composable BoxScope.(Boolean) -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    // A new source must never display the previous source's decoded pixels or loading state.
    key(path, coverAction, blurred) {
        var localChecked by remember { mutableStateOf(path == null || coverAction == "REMOVE") }
        val bitmap by produceState<ImageBitmap?>(null) {
            if (path != null && coverAction != "REMOVE") value = withContext(Dispatchers.IO) {
                fun decode(candidate: String): ImageBitmap? = try {
                    require(candidate.startsWith("covers/") && !candidate.contains(".."))
                    val bytes = DeviceCipher(context).decrypt(File(context.filesDir, candidate).readBytes())
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                (if (blurred) decode("$path.blur.jpg") else null) ?: decode(path)
            }
            localChecked = true
        }
        val remote = resolvePublishedCoverUrl(coverAction, bitmap != null, remoteUrl).takeIf { localChecked }
        var remoteLoaded by remember(remote) { mutableStateOf(false) }
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (bitmap != null) Image(checkNotNull(bitmap), description, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            else if (remote != null) AsyncImage(
                model = ImageRequest.Builder(context).data(remote).diskCachePolicy(CachePolicy.DISABLED).build(),
                contentDescription = description, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop,
                onSuccess = { remoteLoaded = true }, onError = { remoteLoaded = false }
            )
            content(bitmap != null || (remote != null && remoteLoaded))
        }
    }
}

internal fun resolvePublishedCoverUrl(action: String, localAvailable: Boolean, url: String?): String? =
    url?.takeIf { action == "KEEP" && !localAvailable && CoverAsset.allowedUrl(it) }
