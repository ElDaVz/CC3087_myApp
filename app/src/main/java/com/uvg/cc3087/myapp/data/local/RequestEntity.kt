package com.uvg.cc3087.myapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// A submission is a historical snapshot and survives editing/deleting its source draft.
@Entity(tableName = "local_requests", primaryKeys = ["formId", "responseId"])
data class RequestEntity(
    val formId: String,
    val responseId: String,
    val formTitle: String,
    val submittedAt: Long,
    val status: String,
    val answersJson: String
)

data class RequestCount(val formId: String, val count: Int)

@Dao
interface RequestDao {
    @Query("SELECT * FROM local_requests ORDER BY submittedAt DESC, responseId ASC")
    fun observeRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM local_requests ORDER BY submittedAt DESC, responseId ASC")
    suspend fun getRequests(): List<RequestEntity>

    @Query("SELECT * FROM local_requests WHERE formId = :formId AND responseId = :responseId")
    suspend fun getDetail(formId: String, responseId: String): RequestEntity?

    @Insert
    suspend fun insert(request: RequestEntity)

    @Query("UPDATE local_requests SET status = :status WHERE formId = :formId AND responseId = :responseId")
    suspend fun updateStatus(formId: String, responseId: String, status: String): Int

    @Query("SELECT formId, COUNT(*) AS count FROM local_requests GROUP BY formId")
    fun observeCounts(): Flow<List<RequestCount>>
}
