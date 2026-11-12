package com.uvg.cc3087.myapp.data.remote

import com.uvg.cc3087.myapp.data.model.CoverAsset
import com.uvg.cc3087.myapp.data.repository.PreparedCover
import com.uvg.cc3087.myapp.data.repository.PublicationError
import com.uvg.cc3087.myapp.data.repository.PublicationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

fun interface CoverUploader { suspend fun upload(image: PreparedCover): CoverAsset }

/** Unsigned, image-only upload. Never send Firebase tokens or log response bodies. */
class CloudinaryCoverUploader(private val cloud: String, private val preset: String) : CoverUploader {
    override suspend fun upload(image: PreparedCover): CoverAsset = withContext(Dispatchers.IO) {
        if (cloud != CoverAsset.CLOUD || !preset.matches(Regex("[A-Za-z0-9_-]{1,100}")))
            throw PublicationException(PublicationError.COVER_NOT_CONFIGURED)
        require(image.bytes.size <= 2 * 1024 * 1024)
        val boundary = "FormLink${UUID.randomUUID()}"
        val connection = URL("https://api.cloudinary.com/v1_1/$cloud/image/upload").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.use { output ->
                fun text(value: String) = output.write(value.toByteArray(Charsets.UTF_8))
                text("--$boundary\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n$preset\r\n")
                text("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"cover.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                output.write(image.bytes)
                text("\r\n--$boundary--\r\n")
            }
            if (connection.responseCode !in 200..299)
                throw PublicationException(PublicationError.COVER_UPLOAD_FAILED)
            val bytes = connection.inputStream.use { it.readBytesLimited(64 * 1024) }
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            require(json.getString("resource_type") == "image")
            CoverAsset.fromWire(mapOf("url" to json.getString("secure_url"), "publicId" to json.getString("public_id"), "hash" to image.hash))!!
        } finally { connection.disconnect() }
    }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val result = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toByteArray()
        require(result.size() + count <= limit)
        result.write(buffer, 0, count)
    }
}
