package com.uvg.cc3087.myapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "guest_imports")
data class GuestImportEntity(@PrimaryKey val formId: String, val ownerId: String, val completed: Boolean = false)

@Dao
interface GuestImportDao {
    @Query("SELECT id FROM form_drafts WHERE ownerId IS NULL AND deleted = 0 AND NOT EXISTS (SELECT 1 FROM guest_imports WHERE formId = form_drafts.id)")
    suspend fun availableGuestIds(): List<String>
    @Query("SELECT * FROM guest_imports WHERE ownerId = :owner")
    fun observe(owner: String): Flow<List<GuestImportEntity>>
    @Query("SELECT * FROM guest_imports WHERE ownerId = :owner AND completed = 0 ORDER BY formId")
    suspend fun pending(owner: String): List<GuestImportEntity>
    @Query("SELECT * FROM guest_imports WHERE formId = :id")
    suspend fun get(id: String): GuestImportEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun reserve(value: GuestImportEntity)
    @Query("UPDATE guest_imports SET completed = 1 WHERE ownerId = :owner AND formId = :id")
    suspend fun complete(owner: String, id: String)
    @Query("DELETE FROM guest_imports WHERE ownerId = :owner AND formId = :id")
    suspend fun discard(owner: String, id: String)
}
