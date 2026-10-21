package com.uvg.cc3087.myapp.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.FormFieldType

@Entity(tableName = "form_drafts")
data class FormDraftEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "form_draft_fields",
    primaryKeys = ["formId", "id"],
    foreignKeys = [ForeignKey(
        entity = FormDraftEntity::class,
        parentColumns = ["id"],
        childColumns = ["formId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["formId", "position"], unique = true)]
)
data class FormFieldEntity(
    val formId: String,
    val id: String,
    val position: Int,
    val type: FormFieldType,
    val title: String,
    val required: Boolean,
    val allowMultipleAnswers: Boolean,
    val options: List<FieldOption>,
    val helpText: String,
    val placeholder: String,
    val includeTime: Boolean,
    val disallowPastDates: Boolean,
    val allowDecimals: Boolean,
    val minimum: String,
    val maximum: String,
    val unit: String
)

data class FormDraftWithFields(
    @Embedded val form: FormDraftEntity,
    @Relation(parentColumn = "id", entityColumn = "formId")
    val fields: List<FormFieldEntity>
)
