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
    @Query("SELECT * FROM form_drafts WHERE ownerId IS NULL AND deleted = 0 ORDER BY updatedAt DESC, id ASC")
    abstract fun observeDrafts(): Flow<List<FormDraftWithFields>>

    @Transaction
    @Query("SELECT * FROM form_drafts WHERE id = :id")
    abstract suspend fun getDraft(id: String): FormDraftWithFields?

    @Query("SELECT COUNT(*) FROM form_drafts WHERE coverPath = :path")
    abstract suspend fun coverReferences(path: String): Int

    @Transaction
    @Query("SELECT * FROM form_drafts WHERE ownerId IS :owner AND deleted = 0 ORDER BY updatedAt DESC, id ASC")
    abstract fun observeOwned(owner: String?): Flow<List<FormDraftWithFields>>

    @Transaction
    @Query("SELECT * FROM form_drafts WHERE ownerId IS :owner")
    abstract suspend fun owned(owner: String?): List<FormDraftWithFields>

    @Query("SELECT COUNT(*) FROM form_drafts WHERE ownerId = :owner AND revision != syncedRevision")
    abstract fun observePendingCount(owner: String): Flow<Int>

    @Query("UPDATE form_drafts SET syncedRevision = :revision WHERE id = :id AND ownerId = :owner AND revision = :revision")
    abstract suspend fun acknowledge(owner: String, id: String, revision: Long): Int

    @Query("DELETE FROM form_drafts WHERE id = :id AND ownerId = :owner AND revision = :revision AND deleted = 1")
    abstract suspend fun acknowledgeDeletion(owner: String, id: String, revision: Long): Int

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
