package com.example.volatiledemandforecasting.data.cache

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

/** One saved API response. The payload is the DTO exactly as the API sent it. */
@Entity(tableName = "cache_entry")
data class CacheEntry(
    @PrimaryKey val key: String,
    val payload: String,
    /** Epoch milliseconds when this response was fetched, shown as "Last synced". */
    val savedAt: Long,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entry WHERE `key` = :key")
    suspend fun get(key: String): CacheEntry?

    @Upsert
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache_entry")
    suspend fun clear()

    @Query("DELETE FROM cache_entry WHERE savedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun dao(): CacheDao
}

/**
 * Read-through cache for the lists and results that can be viewed offline (spec §13): products,
 * import summaries and forecasts. A live answer is always preferred; the saved copy is used only
 * when the network can't be reached. A real server answer (even an error) is never replaced by a
 * saved copy.
 */
class ResponseCache(
    private val dao: CacheDao,
    private val json: Json,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _servedFromCache = MutableStateFlow<Long?>(null)

    /** When the copy on screen was fetched, or null while the screen shows live data. */
    val servedFromCache: StateFlow<Long?> = _servedFromCache.asStateFlow()

    /**
     * Returns [fetch]'s result and saves it if [cacheable]. On an `IOException` it returns the saved
     * copy for [key] instead, or rethrows if there isn't one.
     */
    suspend fun <T> load(
        key: String,
        serializer: KSerializer<T>,
        cacheable: (T) -> Boolean = { true },
        fetch: suspend () -> T,
    ): T {
        try {
            val fresh = fetch()
            _servedFromCache.value = null
            if (cacheable(fresh)) save(key, serializer, fresh)
            return fresh
        } catch (e: IOException) {
            val entry = try { dao.get(key) } catch (read: CancellationException) { throw read } catch (read: Exception) { null }
            val saved = entry?.let { runCatching { json.decodeFromString(serializer, it.payload) }.getOrNull() } ?: throw e
            _servedFromCache.value = entry.savedAt
            return saved
        }
    }

    private suspend fun <T> save(key: String, serializer: KSerializer<T>, value: T) {
        try {
            dao.put(CacheEntry(key, json.encodeToString(serializer, value), now()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A full disk or a broken database must not fail a request that succeeded.
        }
    }

    /** Sign-out removes everything saved for the user (spec §14). */
    suspend fun clear() {
        _servedFromCache.value = null
        dao.clear()
    }

    suspend fun pruneOlderThan(maxAgeMillis: Long) = dao.deleteOlderThan(now() - maxAgeMillis)
}
