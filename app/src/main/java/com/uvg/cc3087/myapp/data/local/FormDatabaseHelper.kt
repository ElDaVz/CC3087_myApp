package com.uvg.cc3087.myapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import org.json.JSONArray
import org.json.JSONObject

data class FullFormDraft(
    val id: String,
    val title: String,
    val status: FormStatus,
    val ownerId: String,
    val fields: List<FormFieldDraft>,
    val responseCount: Int = 0,
    val updatedDate: String = "Hoy",
    val imageUrl: String = "https://picsum.photos/seed/$id/160/160"
)

class FormDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "form_link.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_FORMS = "forms"
        private const val TABLE_FIELDS = "form_fields"

        private const val COL_FORM_ID = "id"
        private const val COL_FORM_TITLE = "title"
        private const val COL_FORM_RESPONSE_COUNT = "response_count"
        private const val COL_FORM_UPDATED_DATE = "updated_date"
        private const val COL_FORM_STATUS = "status"
        private const val COL_FORM_IMAGE_URL = "image_url"
        private const val COL_FORM_OWNER_ID = "owner_id"
        private const val COL_FORM_UPDATED_AT = "updated_at"

        private const val COL_FIELD_ID = "id"
        private const val COL_FIELD_FORM_ID = "form_id"
        private const val COL_FIELD_TYPE = "type"
        private const val COL_FIELD_TITLE = "title"
        private const val COL_FIELD_REQUIRED = "required"
        private const val COL_FIELD_ALLOW_MULTIPLE = "allow_multiple_answers"
        private const val COL_FIELD_OPTIONS_JSON = "options_json"
        private const val COL_FIELD_POSITION = "position"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_FORMS (
                $COL_FORM_ID TEXT PRIMARY KEY,
                $COL_FORM_TITLE TEXT NOT NULL,
                $COL_FORM_RESPONSE_COUNT INTEGER NOT NULL DEFAULT 0,
                $COL_FORM_UPDATED_DATE TEXT NOT NULL,
                $COL_FORM_STATUS TEXT NOT NULL,
                $COL_FORM_IMAGE_URL TEXT NOT NULL,
                $COL_FORM_OWNER_ID TEXT NOT NULL DEFAULT '',
                $COL_FORM_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_FIELDS (
                $COL_FIELD_ID TEXT PRIMARY KEY,
                $COL_FIELD_FORM_ID TEXT NOT NULL,
                $COL_FIELD_TYPE TEXT NOT NULL,
                $COL_FIELD_TITLE TEXT NOT NULL,
                $COL_FIELD_REQUIRED INTEGER NOT NULL DEFAULT 0,
                $COL_FIELD_ALLOW_MULTIPLE INTEGER NOT NULL DEFAULT 1,
                $COL_FIELD_OPTIONS_JSON TEXT NOT NULL DEFAULT '[]',
                $COL_FIELD_POSITION INTEGER NOT NULL,
                FOREIGN KEY($COL_FIELD_FORM_ID) REFERENCES $TABLE_FORMS($COL_FORM_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FORMS")
        onCreate(db)
    }

    fun hasForms(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_FORMS", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count > 0
    }

    fun insertOrUpdateForm(draft: FullFormDraft) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val formValues = ContentValues().apply {
                put(COL_FORM_ID, draft.id)
                put(COL_FORM_TITLE, draft.title)
                put(COL_FORM_RESPONSE_COUNT, draft.responseCount)
                put(COL_FORM_UPDATED_DATE, draft.updatedDate)
                put(COL_FORM_STATUS, draft.status.name)
                put(COL_FORM_IMAGE_URL, draft.imageUrl)
                put(COL_FORM_OWNER_ID, draft.ownerId)
                put(COL_FORM_UPDATED_AT, System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                TABLE_FORMS,
                null,
                formValues,
                SQLiteDatabase.CONFLICT_REPLACE
            )

            db.delete(TABLE_FIELDS, "$COL_FIELD_FORM_ID = ?", arrayOf(draft.id))

            draft.fields.forEachIndexed { index, field ->
                val fieldValues = ContentValues().apply {
                    put(COL_FIELD_ID, field.id)
                    put(COL_FIELD_FORM_ID, draft.id)
                    put(COL_FIELD_TYPE, field.type.name)
                    put(COL_FIELD_TITLE, field.title)
                    put(COL_FIELD_REQUIRED, if (field.required) 1 else 0)
                    put(COL_FIELD_ALLOW_MULTIPLE, if (field.allowMultipleAnswers) 1 else 0)
                    put(COL_FIELD_OPTIONS_JSON, serializeOptions(field.options))
                    put(COL_FIELD_POSITION, index)
                }
                db.insert(TABLE_FIELDS, null, fieldValues)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllForms(): List<FormSummary> {
        val db = readableDatabase
        val list = mutableListOf<FormSummary>()
        val cursor = db.query(
            TABLE_FORMS,
            null,
            null,
            null,
            null,
            null,
            "$COL_FORM_UPDATED_AT DESC"
        )
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_TITLE))
            val responseCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FORM_RESPONSE_COUNT))
            val updatedDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_UPDATED_DATE))
            val statusStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_STATUS))
            val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_IMAGE_URL))

            val status = try {
                FormStatus.valueOf(statusStr)
            } catch (_: Exception) {
                FormStatus.DRAFT
            }

            list.add(
                FormSummary(
                    id = id,
                    title = title,
                    responseCount = responseCount,
                    updatedDate = updatedDate,
                    status = status,
                    imageUrl = imageUrl
                )
            )
        }
        cursor.close()
        return list
    }

    fun getForm(formId: String): FullFormDraft? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_FORMS,
            null,
            "$COL_FORM_ID = ?",
            arrayOf(formId),
            null,
            null,
            null
        )
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_TITLE))
        val responseCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FORM_RESPONSE_COUNT))
        val updatedDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_UPDATED_DATE))
        val statusStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_STATUS))
        val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_IMAGE_URL))
        val ownerId = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORM_OWNER_ID))
        cursor.close()

        val fields = mutableListOf<FormFieldDraft>()
        val fieldsCursor = db.query(
            TABLE_FIELDS,
            null,
            "$COL_FIELD_FORM_ID = ?",
            arrayOf(formId),
            null,
            null,
            "$COL_FIELD_POSITION ASC"
        )

        while (fieldsCursor.moveToNext()) {
            val fieldId = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow(COL_FIELD_ID))
            val typeStr = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow(COL_FIELD_TYPE))
            val fieldTitle = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow(COL_FIELD_TITLE))
            val required = fieldsCursor.getInt(fieldsCursor.getColumnIndexOrThrow(COL_FIELD_REQUIRED)) == 1
            val allowMultiple = fieldsCursor.getInt(fieldsCursor.getColumnIndexOrThrow(COL_FIELD_ALLOW_MULTIPLE)) == 1
            val optionsJson = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow(COL_FIELD_OPTIONS_JSON))

            val type = try {
                FormFieldType.valueOf(typeStr)
            } catch (_: Exception) {
                FormFieldType.TEXT
            }

            fields.add(
                FormFieldDraft(
                    id = fieldId,
                    type = type,
                    title = fieldTitle,
                    required = required,
                    allowMultipleAnswers = allowMultiple,
                    options = deserializeOptions(optionsJson)
                )
            )
        }
        fieldsCursor.close()

        val status = try {
            FormStatus.valueOf(statusStr)
        } catch (_: Exception) {
            FormStatus.DRAFT
        }

        return FullFormDraft(
            id = id,
            title = title,
            status = status,
            ownerId = ownerId,
            fields = fields,
            responseCount = responseCount,
            updatedDate = updatedDate,
            imageUrl = imageUrl
        )
    }

    fun deleteForm(formId: String) {
        val db = writableDatabase
        db.delete(TABLE_FIELDS, "$COL_FIELD_FORM_ID = ?", arrayOf(formId))
        db.delete(TABLE_FORMS, "$COL_FORM_ID = ?", arrayOf(formId))
    }

    private fun serializeOptions(options: List<FieldOption>): String {
        val array = JSONArray()
        options.forEach { opt ->
            val obj = JSONObject().apply {
                put("id", opt.id)
                put("label", opt.label)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeOptions(json: String): List<FieldOption> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<FieldOption>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FieldOption(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        label = obj.optString("label", "")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}
