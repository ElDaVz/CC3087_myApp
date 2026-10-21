package com.uvg.cc3087.myapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FormDraftDao {
    @Transaction
    @Query("SELECT * FROM form_drafts ORDER BY updatedAt DESC, id ASC")
    abstract fun observeDrafts(): Flow<List<FormDraftWithFields>>

    @Transaction
    @Query("SELECT * FROM form_drafts WHERE id = :id")
    abstract suspend fun getDraft(id: String): FormDraftWithFields?

    @Upsert
    protected abstract suspend fun upsertForm(form: FormDraftEntity)

    @Insert
    protected abstract suspend fun insertFields(fields: List<FormFieldEntity>)

    @Query("DELETE FROM form_draft_fields WHERE formId = :id")
    protected abstract suspend fun deleteFields(id: String)

    @Query("DELETE FROM form_drafts WHERE id = :id")
    abstract suspend fun deleteDraft(id: String)

    @Transaction
    open suspend fun saveDraft(form: FormDraftEntity, fields: List<FormFieldEntity>) {
        // el título y los campos cambian juntos, nunca dejamos medio borrador guardado
        upsertForm(form)
        deleteFields(form.id)
        insertFields(fields)
    }
}
