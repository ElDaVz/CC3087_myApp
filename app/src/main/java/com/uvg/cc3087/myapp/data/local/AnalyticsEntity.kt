package com.uvg.cc3087.myapp.data.local

import androidx.room.*

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(@PrimaryKey val id: String, val name: String, val parametersJson: String,
    @ColumnInfo(defaultValue = "0") val dispatched: Boolean = false)
@Dao
interface AnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun enqueue(row: AnalyticsEventEntity): Long
    @Query("SELECT * FROM analytics_events WHERE dispatched = 0 ORDER BY rowid") suspend fun pending(): List<AnalyticsEventEntity>
    @Query("SELECT COUNT(*) FROM analytics_events WHERE id = :id") suspend fun exists(id: String): Int
    @Query("UPDATE analytics_events SET dispatched = 1 WHERE id = :id AND dispatched = 0") suspend fun claim(id: String): Int
}
