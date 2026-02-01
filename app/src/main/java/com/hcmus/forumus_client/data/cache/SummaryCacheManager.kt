package com.hcmus.forumus_client.data.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import java.util.Base64

/** Client-side cache for AI-generated post summaries. */
class SummaryCacheManager private constructor(context: Context) {

    companion object {
        private const val TAG = "SummaryCacheManager"
        private const val PREFS_NAME = "ai_summary_cache"
        private const val KEY_PREFIX = "summary_"
        private const val DEFAULT_TTL_MILLIS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_CACHE_SIZE = 100 // Maximum cached summaries
        private const val KEY_CACHE_METADATA = "cache_metadata"

        @Volatile
        private var instance: SummaryCacheManager? = null

        fun getInstance(context: Context): SummaryCacheManager {
            return instance ?: synchronized(this) {
                instance ?: SummaryCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /** Represents a cached summary entry. */
    @JsonClass(generateAdapter = true)
    data class CachedSummaryEntry(
        val postId: String,
        val summary: String,
        val contentHash: String,
        val generatedAt: Long,
        val expiresAt: Long,
        val lastAccessedAt: Long = System.currentTimeMillis(),
        val hitCount: Int = 0
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
        
        fun withAccess(): CachedSummaryEntry = copy(
            lastAccessedAt = System.currentTimeMillis(),
            hitCount = hitCount + 1
        )
    }

    /** Cache metadata for tracking statistics. */
    @JsonClass(generateAdapter = true)
    data class CacheMetadata(
        val totalHits: Long = 0,
        val totalMisses: Long = 0,
        val totalSaves: Long = 0,
        val lastCleanup: Long = System.currentTimeMillis()
    ) {
        fun getHitRate(): Double {
            val total = totalHits + totalMisses
            return if (total > 0) totalHits.toDouble() / total else 0.0
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val entryAdapter = moshi.adapter(CachedSummaryEntry::class.java)
    private val metadataAdapter = moshi.adapter(CacheMetadata::class.java)

    /** Gets cached summary if valid. */
    @Synchronized
    fun get(postId: String, contentHash: String? = null): CachedSummaryEntry? {
        val key = KEY_PREFIX + postId
        val json = prefs.getString(key, null) ?: run {
            recordMiss()
            return null
        }

        return try {
            val entry = entryAdapter.fromJson(json) ?: run {
                recordMiss()
                return null
            }

            // Check expiration
            if (entry.isExpired()) {
                Log.d(TAG, "Cache expired for post $postId")
                remove(postId)
                recordMiss()
                return null
            }

            // Validate content hash if provided
            if (contentHash != null && entry.contentHash != contentHash) {
                Log.d(TAG, "Content hash mismatch for post $postId - content changed")
                remove(postId)
                recordMiss()
                return null
            }

            // Valid cache hit - update access stats
            val updatedEntry = entry.withAccess()
            saveEntry(postId, updatedEntry)
            recordHit()
            
            Log.d(TAG, "Cache HIT for post $postId (hits: ${updatedEntry.hitCount})")
            updatedEntry
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cache for post $postId: ${e.message}")
            remove(postId)
            recordMiss()
            null
        }
    }

    /** Stores a summary in the cache. */
    @Synchronized
    fun put(
        postId: String,
        summary: String,
        contentHash: String,
        generatedAt: Long,
        expiresAt: Long? = null
    ) {
        // Ensure cache doesn't exceed size limit
        ensureCacheSize()

        val entry = CachedSummaryEntry(
            postId = postId,
            summary = summary,
            contentHash = contentHash,
            generatedAt = generatedAt,
            expiresAt = expiresAt ?: (generatedAt + DEFAULT_TTL_MILLIS)
        )

        saveEntry(postId, entry)
        recordSave()
        
        Log.d(TAG, "Cached summary for post $postId (expires: ${entry.expiresAt})")
    }

    private fun saveEntry(postId: String, entry: CachedSummaryEntry) {
        val key = KEY_PREFIX + postId
        val json = entryAdapter.toJson(entry)
        prefs.edit().putString(key, json).apply()
    }

    /** Removes a cached summary. */
    @Synchronized
    fun remove(postId: String) {
        val key = KEY_PREFIX + postId
        prefs.edit().remove(key).apply()
        Log.d(TAG, "Removed cache for post $postId")
    }

    /** Checks if valid cache exists for a post. */
    fun hasValidCache(postId: String): Boolean {
        return get(postId) != null
    }

    /** Clears all cached summaries. */
    @Synchronized
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared all cached summaries")
    }

    /** Gets the number of cached entries. */
    fun size(): Int {
        return prefs.all.count { it.key.startsWith(KEY_PREFIX) }
    }

    /** Gets cache statistics. */
    fun getStats(): CacheMetadata {
        val json = prefs.getString(KEY_CACHE_METADATA, null) ?: return CacheMetadata()
        return try {
            metadataAdapter.fromJson(json) ?: CacheMetadata()
        } catch (e: Exception) {
            CacheMetadata()
        }
    }

    /** Gets a human-readable cache status summary. */
    fun getCacheStatusSummary(): String {
        val stats = getStats()
        return "Cache: size=${size()}, hits=${stats.totalHits}, " +
               "misses=${stats.totalMisses}, hitRate=${String.format("%.1f", stats.getHitRate() * 100)}%"
    }

    /** Cleans up expired entries. */
    @Synchronized
    fun cleanup() {
        val editor = prefs.edit()
        var removed = 0

        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX) && value is String) {
                try {
                    val entry = entryAdapter.fromJson(value)
                    if (entry?.isExpired() == true) {
                        editor.remove(key)
                        removed++
                    }
                } catch (e: Exception) {
                    editor.remove(key)
                    removed++
                }
            }
        }

        editor.apply()
        updateMetadata { it.copy(lastCleanup = System.currentTimeMillis()) }
        
        if (removed > 0) {
            Log.d(TAG, "Cleanup removed $removed expired entries")
        }
    }

    /** Ensures cache size doesn't exceed limit. */
    private fun ensureCacheSize() {
        val currentSize = size()
        if (currentSize < MAX_CACHE_SIZE) return

        // Get all entries sorted by last access time
        val entries = prefs.all
            .filter { it.key.startsWith(KEY_PREFIX) }
            .mapNotNull { (key, value) ->
                try {
                    val entry = entryAdapter.fromJson(value as String)
                    key to entry
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.second?.lastAccessedAt ?: 0L }

        // Remove oldest 20% of entries
        val toRemove = (MAX_CACHE_SIZE * 0.2).toInt().coerceAtLeast(1)
        val editor = prefs.edit()
        
        entries.take(toRemove).forEach { (key, _) ->
            editor.remove(key)
        }
        
        editor.apply()
        Log.d(TAG, "Evicted $toRemove oldest cache entries")
    }

    private fun recordHit() {
        updateMetadata { it.copy(totalHits = it.totalHits + 1) }
    }

    private fun recordMiss() {
        updateMetadata { it.copy(totalMisses = it.totalMisses + 1) }
    }

    private fun recordSave() {
        updateMetadata { it.copy(totalSaves = it.totalSaves + 1) }
    }

    private fun updateMetadata(update: (CacheMetadata) -> CacheMetadata) {
        val current = getStats()
        val updated = update(current)
        val json = metadataAdapter.toJson(updated)
        prefs.edit().putString(KEY_CACHE_METADATA, json).apply()
    }

    /** Computes a content hash for validation. */
    fun computeContentHash(title: String?, content: String?): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val combined = "${title ?: ""}|${content ?: ""}"
            val hashBytes = digest.digest(combined.toByteArray())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Base64.getEncoder().encodeToString(hashBytes)
            } else {
                android.util.Base64.encodeToString(hashBytes, android.util.Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            // Fallback to simple hash
            "${title ?: ""}|${content ?: ""}".hashCode().toString()
        }
    }
}
