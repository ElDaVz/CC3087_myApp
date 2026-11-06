package com.uvg.cc3087.myapp.data.remote.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Error payloads and URLs can contain PII or credentials: never expose them in logs/UI. */
class BackendHttpException(val status: Int, val code: String) : Exception("Backend request failed ($status)")
class FirebaseHttp(private val allowEmulators: Boolean = false) {
    suspend fun request(url: String, body: JSONObject? = null, token: String? = null,
        method: String = if (body == null) "GET" else "POST"): JSONObject = withContext(Dispatchers.IO) {
        val endpoint = URL(url)
        require(endpoint.protocol == "https" || (allowEmulators && endpoint.host in setOf("10.0.2.2", "127.0.0.1", "localhost")))
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000; connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) { connection.doOutput = true; connection.outputStream.use { it.write(body.toString().toByteArray()) } }
            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val result = if (raw.isBlank()) JSONObject() else if (raw.trimStart().startsWith("[")) JSONObject().put("rows", org.json.JSONArray(raw)) else JSONObject(raw)
            if (status !in 200..299) throw BackendHttpException(status,
                result.optJSONObject("error")?.optString("message")?.substringBefore(':')?.takeIf { it.matches(Regex("[A-Z_]+")) } ?: "REQUEST_FAILED")
            result
        } finally { connection.disconnect() }
    }
}
