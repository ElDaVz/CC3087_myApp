package com.uvg.cc3087.myapp.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.uvg.cc3087.myapp.data.model.OwnerProfile
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "owner_profiles")
data class OwnerProfileEntity(
    @PrimaryKey val ownerId: String,
    val personalName: String = "",
    val personalEmail: String = "",
    val businessName: String = "",
    val businessEmail: String = "",
    val logoPath: String? = null,
    val revision: Long = 0,
    val syncedRevision: Long = 0
)

fun OwnerProfileEntity.toModel() = OwnerProfile(
    ownerId, personalName, personalEmail, businessName, businessEmail, logoPath, revision, syncedRevision
)

fun OwnerProfile.toEntity() = OwnerProfileEntity(
    ownerId, personalName, personalEmail, businessName, businessEmail, logoPath, revision, syncedRevision
)

@Dao
interface OwnerProfileDao {
    @Query("SELECT * FROM owner_profiles WHERE ownerId = :ownerId")
    fun observe(ownerId: String): Flow<OwnerProfileEntity?>

    @Query("SELECT * FROM owner_profiles WHERE ownerId = :ownerId")
    suspend fun get(ownerId: String): OwnerProfileEntity?

    @Query("SELECT COUNT(*) FROM owner_profiles WHERE ownerId = :ownerId AND revision != syncedRevision")
    fun observePending(ownerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM owner_profiles WHERE ownerId = :ownerId AND revision != syncedRevision")
    suspend fun pending(ownerId: String): Int

    @Upsert
    suspend fun save(row: OwnerProfileEntity)

    @Query("UPDATE owner_profiles SET syncedRevision = :revision WHERE ownerId = :ownerId AND revision = :revision")
    suspend fun acknowledge(ownerId: String, revision: Long)
}
