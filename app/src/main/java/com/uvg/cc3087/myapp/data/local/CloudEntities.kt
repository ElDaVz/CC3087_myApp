package com.uvg.cc3087.myapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "form_publications")
data class PublicationEntity(@PrimaryKey val formId: String, val ownerId: String, val status: String,
    val url: String, val publishedAt: Long, val version: Long, val fingerprint: String,
    val firstPublishedAt: Long)

@Entity(tableName = "remote_requests", primaryKeys = ["ownerId", "formId", "responseId"])
data class RemoteRequestEntity(val ownerId: String, val formId: String, val responseId: String,
    val formTitle: String, val submittedAt: Long, val status: String, val answersJson: String,
    @ColumnInfo(defaultValue = "0") val statusRevision: Long = 0,
    @ColumnInfo(defaultValue = "0") val syncedStatusRevision: Long = 0)

@Dao
interface CloudDao {
    @Query("SELECT * FROM form_publications WHERE formId = :id AND ownerId = :owner")
    suspend fun publication(owner: String, id: String): PublicationEntity?
    @Query("SELECT * FROM form_publications WHERE ownerId = :owner")
    fun observePublications(owner: String): Flow<List<PublicationEntity>>
    @Upsert suspend fun savePublication(row: PublicationEntity)
    @Query("SELECT * FROM remote_requests WHERE ownerId = :owner ORDER BY submittedAt DESC")
    fun observeRequests(owner: String): Flow<List<RemoteRequestEntity>>
    @Query("SELECT * FROM remote_requests WHERE ownerId = :owner ORDER BY submittedAt DESC")
    suspend fun requests(owner: String): List<RemoteRequestEntity>
    @Query("SELECT * FROM remote_requests WHERE ownerId = :owner AND formId = :form AND responseId = :response")
    suspend fun detail(owner: String, form: String, response: String): RemoteRequestEntity?
    @Upsert suspend fun saveRequest(row: RemoteRequestEntity)
    @Query("UPDATE remote_requests SET syncedStatusRevision = :revision WHERE ownerId = :owner AND formId = :form AND responseId = :response AND statusRevision = :revision")
    suspend fun acknowledge(owner: String, form: String, response: String, revision: Long)
}
