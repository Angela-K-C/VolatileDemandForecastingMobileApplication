package com.example.volatiledemandforecasting.data

import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.Network
import com.example.volatiledemandforecasting.data.cache.CacheDao
import com.example.volatiledemandforecasting.data.cache.CacheEntry
import com.example.volatiledemandforecasting.data.cache.ResponseCache
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

@Serializable
private data class Item(val id: String, val units: Double? = null)

private class FakeDao : CacheDao {
    val rows = mutableMapOf<String, CacheEntry>()
    override suspend fun get(key: String) = rows[key]
    override suspend fun put(entry: CacheEntry) { rows[entry.key] = entry }
    override suspend fun clear() = rows.clear()
    override suspend fun deleteOlderThan(before: Long) { rows.values.removeAll { it.savedAt < before } }
}

class ResponseCacheTest {
    private val dao = FakeDao()
    private var clock = 1_000L
    private val cache = ResponseCache(dao, Network.json) { clock }

    private suspend fun load(key: String = "k", cacheable: (Item) -> Boolean = { true }, fetch: suspend () -> Item) =
        cache.load(key, Item.serializer(), cacheable, fetch)

    @Test
    fun `a live answer is returned, saved and clears the offline notice`() = runBlocking {
        val result = load { Item("a", 3.0) }
        assertEquals(Item("a", 3.0), result)
        assertEquals(1_000L, dao.rows.getValue("k").savedAt)
        assertNull(cache.servedFromCache.value)
    }

    @Test
    fun `offline the saved copy is returned with its time`() = runBlocking {
        load { Item("a", null) }
        clock = 9_000L
        val result = load { throw IOException("no network") }
        assertEquals(Item("a", null), result)
        assertEquals(1_000L, cache.servedFromCache.value)
    }

    @Test
    fun `offline with nothing saved still fails`() = runBlocking {
        try {
            load("other") { throw IOException("no network") }
            fail("expected IOException")
        } catch (e: IOException) {
            assertNull(cache.servedFromCache.value)
        }
    }

    @Test
    fun `a server error is never replaced by a saved copy`() = runBlocking {
        load { Item("a") }
        try {
            load { throw ApiException(403, "FORBIDDEN", "no", null, null) }
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertNull(cache.servedFromCache.value)
        }
    }

    @Test
    fun `results that are not final are not saved`() = runBlocking {
        load(cacheable = { false }) { Item("pending") }
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `sign-out clears everything and old entries are pruned`() = runBlocking {
        load("old") { Item("1") }
        clock = 100_000L
        load("new") { Item("2") }
        cache.pruneOlderThan(50_000L)
        assertEquals(setOf("new"), dao.rows.keys)
        cache.clear()
        assertTrue(dao.rows.isEmpty())
    }
}
