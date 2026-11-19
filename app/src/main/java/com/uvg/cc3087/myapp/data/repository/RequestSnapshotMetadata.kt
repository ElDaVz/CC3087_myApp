package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Locale

internal fun remoteAnswerNumber(value: Any?): BigDecimal? = when (value) {
    // el formulario web conserva los números como strings canónicos
    is String -> parseNumber(value, Locale.ROOT)
    is Number -> value.toString().toBigDecimalOrNull()
    else -> null
}

internal fun JSONObject.withFieldSnapshot(field: FormFieldDraft): JSONObject = put("field", JSONObject()
    .put("type", field.type.name)
    .put("options", JSONArray(if (field.type.hasOptions) field.options.map { it.label } else emptyList<String>()))
    .put("unit", if (field.type == FormFieldType.NUMBER) field.unit else "")
    .put("includeTime", field.type == FormFieldType.DATE && field.includeTime)
    .put("multipleAnswers", field.isMultipleSelection))

internal fun JSONObject.readFieldSnapshot(): RequestFieldSnapshot? {
    val field = optJSONObject("field") ?: return null
    val type = FormFieldType.entries.find { it.name == field.optString("type") } ?: return null
    val options = field.optJSONArray("options")
    return RequestFieldSnapshot(type,
        if (type.hasOptions && options != null) (0 until options.length()).map { options.getString(it) } else emptyList(),
        if (type == FormFieldType.NUMBER) field.optString("unit") else "",
        type == FormFieldType.DATE && field.optBoolean("includeTime"),
        field.optBoolean("multipleAnswers", type == FormFieldType.CHECKBOX_GROUP))
}

internal fun JSONObject.withRemoteFieldSnapshot(field: Map<String, Any?>): JSONObject {
    val type = when (val wireType = field["type"]) {
        "SHORT_TEXT" -> FormFieldType.TEXT
        "DATE_TIME" -> FormFieldType.DATE
        else -> FormFieldType.entries.find { it.name == wireType }
    } ?: return this
    val config = field["config"] as? Map<*, *> ?: return this
    val options = (config["options"] as? List<*>)?.mapNotNull {
        val option = it as? Map<*, *> ?: return@mapNotNull null
        (option["label"] as? String)?.let { label -> FieldOption(label = label) }
    }.orEmpty()
    return withFieldSnapshot(FormFieldDraft(field["id"] as String, type, field["label"] as String,
        options = options, unit = config["unit"] as? String ?: "", includeTime = config["includeTime"] == true,
        allowMultipleAnswers = config["allowMultipleAnswers"] == true))
}
