package com.uvg.cc3087.myapp.data.local

import androidx.room.TypeConverter
import com.uvg.cc3087.myapp.data.model.FieldOption
import org.json.JSONArray
import org.json.JSONObject

class FieldOptionConverters {
    @TypeConverter
    fun encode(options: List<FieldOption>): String = JSONArray().apply {
        options.forEach { option ->
            put(JSONObject().put("id", option.id).put("label", option.label))
        }
    }.toString()

    @TypeConverter
    fun decode(value: String): List<FieldOption> {
        val options = JSONArray(value)
        return List(options.length()) { index ->
            val option = options.getJSONObject(index)
            FieldOption(id = option.getString("id"), label = option.getString("label"))
        }
    }
}
