package com.uvg.cc3087.myapp.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.uvg.cc3087.myapp.data.repository.PreparedCover
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max

/** Business logos stay encrypted in private files; only an optimized copy is ever uploaded. */
class PrivateLogoStorage(private val context: Context) {
    suspend fun importImage(source: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "logos").apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        try {
            val bytes = context.contentResolver.openInputStream(source.toUri()).use { stream ->
                checkNotNull(stream) { "Image is unavailable" }
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 10 * 1024 * 1024) { "Image exceeds 10 MiB" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
            val decoded = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample }))
            val normalized = createBitmap(decoded.width, decoded.height)
            Canvas(normalized).apply { drawColor(Color.WHITE); drawBitmap(decoded, 0f, 0f, null) }
            target.writeBytes(DeviceCipher(context).encrypt(ByteArrayOutputStream().also {
                check(normalized.compress(Bitmap.CompressFormat.JPEG, 85, it))
            }.toByteArray()))
            decoded.recycle()
            normalized.recycle()
            "logos/${target.name}"
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    suspend fun prepareUpload(path: String): PreparedCover = withContext(Dispatchers.IO) {
        check(path.matches(Regex("logos/[a-fA-F0-9-]+[.]jpg"))) { "Invalid logo path" }
        val encrypted = File(context.filesDir, path).readBytes()
        check(encrypted.size <= 20 * 1024 * 1024) { "Logo file is too large" }
        val plain = DeviceCipher(context).decrypt(encrypted)
        val decoded = checkNotNull(BitmapFactory.decodeByteArray(plain, 0, plain.size))
        val factor = minOf(1f, 512f / max(decoded.width, decoded.height))
        val scaled = decoded.scale(max(1, (decoded.width * factor).toInt()), max(1, (decoded.height * factor).toInt()))
        try {
            val bytes = ByteArrayOutputStream().also { check(scaled.compress(Bitmap.CompressFormat.JPEG, 80, it)) }.toByteArray()
            check(bytes.size <= 2 * 1024 * 1024) { "Logo exceeds 2 MiB" }
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            PreparedCover(bytes, hash)
        } finally {
            decoded.recycle()
            scaled.recycle()
        }
    }

    suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        if (path == null) return@withContext
        check(path.startsWith("logos/") && !path.contains("..")) { "Invalid logo path" }
        File(context.filesDir, path).delete()
        Unit
    }

    suspend fun decode(path: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (path == null) return@withContext null
        try {
            check(path.startsWith("logos/") && !path.contains(".."))
            val plain = DeviceCipher(context).decrypt(File(context.filesDir, path).readBytes())
            BitmapFactory.decodeByteArray(plain, 0, plain.size)
        } catch (_: Exception) {
            null
        }
    }

}
