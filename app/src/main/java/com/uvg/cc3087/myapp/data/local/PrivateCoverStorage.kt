package com.uvg.cc3087.myapp.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.uvg.cc3087.myapp.data.repository.CoverStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/** The database stores relative paths; the files stay in private, durable app storage. */
class PrivateCoverStorage(private val context: Context) : CoverStorage {
    override suspend fun importImage(source: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "covers").apply { mkdirs() }
        val cipher = com.uvg.cc3087.myapp.data.security.DeviceCipher(context)
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        try {
            val bytes = context.contentResolver.openInputStream(Uri.parse(source)).use { stream ->
                requireNotNull(stream) { "Image is unavailable" }
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 10 * 1024 * 1024) { "Image exceeds 10 MiB" }
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                require(bytes.size <= 10 * 1024 * 1024) { "Image exceeds 10 MiB" }
                bytes
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
            val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample }))
            val exif = runCatching { ExifInterface(java.io.ByteArrayInputStream(bytes)) }.getOrNull()
            val matrix = Matrix().apply {
                when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                    2 -> setScale(-1f, 1f)
                    3 -> setRotate(180f)
                    4 -> setScale(1f, -1f)
                    5 -> { setRotate(90f); postScale(-1f, 1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(270f); postScale(-1f, 1f) }
                    8 -> setRotate(270f)
                }
            }
            val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val normalized = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
            Canvas(normalized).apply { drawColor(Color.WHITE); drawBitmap(oriented, 0f, 0f, null) }
            target.writeBytes(cipher.encrypt(java.io.ByteArrayOutputStream().also { check(normalized.compress(Bitmap.CompressFormat.JPEG, 85, it)) }.toByteArray()))
            val small = Bitmap.createScaledBitmap(normalized, 96,
                max(1, 96 * normalized.height / normalized.width).coerceAtMost(192), true)
            val blurred = blur(small)
            File(target.path + ".blur.jpg").writeBytes(cipher.encrypt(java.io.ByteArrayOutputStream().also {
                check(blurred.compress(Bitmap.CompressFormat.JPEG, 85, it))
            }.toByteArray()))
            setOf(decoded, oriented, normalized, small, blurred).forEach { it.recycle() }
            "covers/${target.name}"
        } catch (error: Exception) {
            target.delete()
            File(target.path + ".blur.jpg").delete()
            throw error
        }
    }

    override suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        require(path.startsWith("covers/") && !path.contains(".."))
        File(context.filesDir, path).delete()
        File(context.filesDir, path + ".blur.jpg").delete()
        Unit
    }

    // Small CPU blur works on every supported API, including Android 26–30.
    private fun blur(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        var pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        repeat(3) {
            val result = IntArray(pixels.size)
            for (y in 0 until h) for (x in 0 until w) {
                var red = 0; var green = 0; var blue = 0; var count = 0
                for (dy in -3..3) for (dx in -3..3) {
                    val color = pixels[(y + dy).coerceIn(0, h - 1) * w + (x + dx).coerceIn(0, w - 1)]
                    red += Color.red(color); green += Color.green(color); blue += Color.blue(color); count++
                }
                result[y * w + x] = Color.rgb(red / count, green / count, blue / count)
            }
            pixels = result
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
