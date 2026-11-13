package com.uvg.cc3087.myapp

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.components.FormListItem
import com.uvg.cc3087.myapp.ui.screens.EditForm
import com.uvg.cc3087.myapp.ui.screens.FormPreviewScreen
import com.uvg.cc3087.myapp.ui.state.EditFormUiState
import com.uvg.cc3087.myapp.ui.theme.MyappTheme
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RestoredCoverUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun cardResolvesCovers() = verifySurface(0)
    @Test fun editorResolvesCovers() = verifySurface(1)
    @Test fun previewResolvesCovers() = verifySurface(2)

    @OptIn(coil3.annotation.DelicateCoilApi::class)
    private fun verifySurface(surface: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previous = SingletonImageLoader.get(context)
        val remote = "https://res.cloudinary.com/uxrkjzti/image/upload/v1/controlled.jpg"
        val requested = AtomicInteger()
        val failures = AtomicInteger()
        val green = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN) }
        val loader = ImageLoader.Builder(context).components {
            add(Interceptor { chain ->
                requested.incrementAndGet()
                if (chain.request.data.toString().contains("failure"))
                    ErrorResult(null, chain.request, java.io.IOException("Controlled failure")).also { failures.incrementAndGet() }
                else SuccessResult(green.asImage(), chain.request)
            })
        }.build()
        SingletonImageLoader.setUnsafe(loader)
        val path = "covers/${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, path).apply { parentFile!!.mkdirs() }
        val red = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        file.writeBytes(DeviceCipher(context).encrypt(ByteArrayOutputStream().also {
            red.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()))
        val state = mutableStateOf(EditFormUiState(title = "Cover test", draftId = "restored"))
        try {
            compose.setContent {
                MyappTheme(dynamicColor = false) {
                    val current = state.value
                    when (surface) {
                        0 -> FormListItem(FormSummary("restored", current.title, 0, "", FormStatus.DRAFT,
                            current.coverPath.orEmpty(), current.publishedCoverUrl, current.coverAction))
                        1 -> EditForm(current, {}, { FormValidationResult.READY }, {}, { emptyMap() },
                            { _, _, _ -> emptyMap() }, { false })
                        else -> FormPreviewScreen(current.title, emptyList(), {}, FormValidationResult.READY,
                            emptyMap(), { _, _, _ -> emptyMap() }, coverPath = current.coverPath,
                            publishedCoverUrl = current.publishedCoverUrl, coverAction = current.coverAction)
                    }
                }
            }
            fun hasColor(red: Boolean): Boolean {
                // Inspect the thumbnail itself: other card actions may legitimately be red.
                val description = context.getString(R.string.thumbnail_for_form, "Cover test")
                if (surface == 0 && compose.onAllNodesWithContentDescription(description,
                        useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) return false
                val target = if (surface == 0) compose.onNodeWithContentDescription(description,
                    useUnmergedTree = true) else compose.onRoot()
                val pixels = target.captureToImage().toPixelMap()
                var count = 0
                for (y in 0 until pixels.height step 4) for (x in 0 until pixels.width step 4) {
                    val c = pixels[x, y]
                    if (if (red) c.red > c.green + .3f && c.red > c.blue + .3f
                        else c.green > c.red + .3f && c.green > c.blue + .3f) count++
                }
                return count > 20
            }
            fun change(path: String? = null, action: String = "KEEP", url: String? = remote) {
                compose.runOnIdle { state.value = state.value.copy(coverPath = path, coverAction = action, publishedCoverUrl = url) }
            }
            assertFalse(hasColor(false))
            // Metadata arriving after the surface opened must render without reopening it.
            change()
            compose.waitUntil(5000) { hasColor(false) }
            if (surface == 1) {
                compose.onNodeWithContentDescription(context.getString(R.string.preview)).performClick()
                compose.waitUntil(5000) { hasColor(false) }
                // Verify the preview BackHandler through its Activity, independent of OS focus.
                compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
                compose.onNodeWithContentDescription(context.getString(R.string.preview)).assertExists()
            }
            change(path)
            compose.waitUntil(5000) { hasColor(true) }
            change(path, "REPLACE")
            compose.waitUntil(5000) { hasColor(true) }
            change(path, "REMOVE")
            compose.waitForIdle()
            assertFalse(hasColor(true)); assertFalse(hasColor(false))
            change("covers/missing.jpg", "REPLACE")
            compose.waitForIdle()
            assertFalse(hasColor(false))
            change("covers/missing.jpg")
            compose.waitUntil(5000) { hasColor(false) }
            change(url = remote.replace("controlled", "failure"))
            compose.waitUntil(5000) { failures.get() > 0 }
            compose.waitForIdle()
            assertFalse(hasColor(false))
            val before = requested.get()
            change(url = "https://example.com/rejected.jpg")
            compose.waitForIdle()
            assertFalse(hasColor(false))
            assertEquals(before, requested.get())
        } finally {
            SingletonImageLoader.setUnsafe(previous)
            loader.shutdown()
            file.delete()
        }
    }
}
