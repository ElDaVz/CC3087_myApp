package com.uvg.cc3087.myapp.data.remote.http

import com.uvg.cc3087.myapp.data.repository.RestAuthRepository
import org.json.JSONArray
import org.json.JSONObject

/** Firestore wire values, update masks and preconditions remain below repository boundaries. */
class FirestoreRest(val project: String, private val auth: RestAuthRepository,
    private val http: FirebaseHttp, emulator: Boolean = false) {
    val database = "projects/$project/databases/(default)"
    private val base = (if (emulator) "http://10.0.2.2:8080" else "https://firestore.googleapis.com") + "/v1/$database/documents"
    fun name(path: String) = "$database/documents/$path"
    suspend fun get(owner: String, path: String): JSONObject? = try {
        http.request("$base/$path", token = auth.token(owner))
    } catch (e: BackendHttpException) { if (e.status == 404) null else throw e }
    suspend fun query(owner: String, collection: String, field: String = "ownerId", allDescendants: Boolean = false): List<JSONObject> {
        val query = JSONObject().put("from", JSONArray().put(JSONObject().put("collectionId", collection).put("allDescendants", allDescendants)))
            .put("where", JSONObject().put("fieldFilter", JSONObject().put("field", JSONObject().put("fieldPath", field))
                .put("op", "EQUAL").put("value", encode(owner))))
        val result = http.request("$base:runQuery", JSONObject().put("structuredQuery", query), auth.token(owner)).optJSONArray("rows") ?: JSONArray()
        return (0 until result.length()).mapNotNull { result.getJSONObject(it).optJSONObject("document") }
    }
    suspend fun commit(owner: String, writes: List<JSONObject>): JSONObject = http.request("$base:commit",
        JSONObject().put("writes", JSONArray(writes)), auth.token(owner))
    fun update(path: String, values: Map<String, Any?>, serverTimes: List<String> = emptyList(),
        precondition: JSONObject? = null): JSONObject = JSONObject().put("update", JSONObject().put("name", name(path))
        .put("fields", JSONObject().apply { values.forEach { (k, v) -> put(k, encode(v)) } }))
        .put("updateMask", JSONObject().put("fieldPaths", JSONArray(values.keys.toList()))).apply {
            if (serverTimes.isNotEmpty()) put("updateTransforms", JSONArray(serverTimes.map {
                JSONObject().put("fieldPath", it).put("setToServerValue", "REQUEST_TIME") }))
            precondition?.let { put("currentDocument", it) }
        }
    companion object {
        fun encode(value: Any?): JSONObject = when (value) {
            null -> JSONObject().put("nullValue", JSONObject.NULL)
            is Boolean -> JSONObject().put("booleanValue", value)
            is Int, is Long -> JSONObject().put("integerValue", value.toString())
            is Number -> JSONObject().put("doubleValue", value)
            is Map<*, *> -> JSONObject().put("mapValue", JSONObject().put("fields", JSONObject().apply { value.forEach { (k, v) -> put(k.toString(), encode(v)) } }))
            is List<*> -> JSONObject().put("arrayValue", JSONObject().put("values", JSONArray(value.map(::encode))))
            is java.time.Instant -> JSONObject().put("timestampValue", value.toString())
            else -> JSONObject().put("stringValue", value.toString())
        }
        fun fields(document: JSONObject): Map<String, Any?> = document.optJSONObject("fields")?.let { data -> data.keys().asSequence().associateWith { decode(data.getJSONObject(it)) } }.orEmpty()
        fun decode(value: JSONObject): Any? = when {
            value.has("nullValue") -> null
            value.has("stringValue") -> value.getString("stringValue")
            value.has("integerValue") -> value.getString("integerValue").toLong()
            value.has("doubleValue") -> value.getDouble("doubleValue")
            value.has("booleanValue") -> value.getBoolean("booleanValue")
            value.has("timestampValue") -> java.time.Instant.parse(value.getString("timestampValue"))
            value.has("mapValue") -> fields(value.getJSONObject("mapValue"))
            value.has("arrayValue") -> value.getJSONObject("arrayValue").optJSONArray("values").let { array ->
                if (array == null) emptyList<Any?>() else (0 until array.length()).map { decode(array.getJSONObject(it)) }
            }
            else -> error("Unsupported remote value")
        }
    }
}
