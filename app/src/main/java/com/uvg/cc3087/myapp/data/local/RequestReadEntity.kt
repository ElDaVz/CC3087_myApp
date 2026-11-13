package com.uvg.cc3087.myapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Device-local reading history. Account scopes cannot collide with the guest partition. */
@Entity(tableName = "request_reads", primaryKeys = ["scope", "formId", "responseId"])
data class RequestReadEntity(val scope: String, val formId: String, val responseId: String)

@Dao
interface RequestReadDao {
    @Query("SELECT * FROM request_reads WHERE scope = :scope")
    fun observe(scope: String): Flow<List<RequestReadEntity>>
    @Query("SELECT * FROM request_reads WHERE scope = :scope")
    suspend fun get(scope: String): List<RequestReadEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markRead(value: RequestReadEntity)
}
