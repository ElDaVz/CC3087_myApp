package com.uvg.cc3087.myapp.data.remote

import com.uvg.cc3087.myapp.data.model.*

/** Explicit wire contract, independent of Compose, Room entities and JVM serialization. */
object FormFieldWire {
    fun encode(field: FormFieldDraft, position: Int): Map<String, Any> = mapOf(
        "id" to field.id, "type" to when (field.type) {
            FormFieldType.TEXT -> "SHORT_TEXT"
            FormFieldType.DATE -> "DATE_TIME"
            else -> field.type.name
        },
        "optionIds" to field.options.map { it.id },
        "label" to field.title, "description" to field.helpText, "required" to field.required,
        "position" to position, "config" to mapOf(
            "placeholder" to field.placeholder, "allowMultipleAnswers" to field.allowMultipleAnswers,
            "includeTime" to field.includeTime, "disallowPastDates" to field.disallowPastDates,
            "allowDecimals" to field.allowDecimals, "minimum" to field.minimum,
            "maximum" to field.maximum, "unit" to field.unit,
            "options" to field.options.map { mapOf("id" to it.id, "label" to it.label) }
        ))
    fun decode(row: Map<*, *>): FormFieldDraft {
        val config = row["config"] as? Map<*, *> ?: error("Missing field configuration")
        fun text(key: String) = config[key] as? String ?: error("Invalid field configuration: $key")
        fun flag(key: String) = config[key] as? Boolean ?: error("Invalid field configuration: $key")
        val type = when (val name = row["type"] as? String) {
            "SHORT_TEXT" -> FormFieldType.TEXT
            "DATE_TIME" -> FormFieldType.DATE
            else -> FormFieldType.valueOf(requireNotNull(name))
        }
        val options = (config["options"] as? List<*>)?.map { item ->
            val option = item as? Map<*, *> ?: error("Invalid option")
            FieldOption(option["id"] as? String ?: error("Missing option ID"),
                option["label"] as? String ?: error("Missing option label"))
        } ?: error("Missing options")
        return FormFieldDraft(id = row["id"] as? String ?: error("Missing field ID"),
            type = type, title = row["label"] as? String ?: error("Missing field label"),
            required = row["required"] as? Boolean ?: error("Missing required flag"),
            helpText = row["description"] as? String ?: "", options = options,
            placeholder = text("placeholder"), allowMultipleAnswers = flag("allowMultipleAnswers"),
            includeTime = flag("includeTime"), disallowPastDates = flag("disallowPastDates"),
            allowDecimals = flag("allowDecimals"), minimum = text("minimum"), maximum = text("maximum"), unit = text("unit"))
    }
}
