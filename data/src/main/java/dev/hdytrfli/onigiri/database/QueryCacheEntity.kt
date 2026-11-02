package dev.hdytrfli.onigiri.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** One persisted SWR query payload. The key is the stable query identity, not a SQL key. */
@Entity(tableName = "query_cache")
data class QueryCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val updatedAt: Long,
)
