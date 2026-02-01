package com.hcmus.forumus_client.data.cache

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/** Manages video file caching to improve performance. */
class VideoCacheManager private constructor(private val context: Context) {

    private val cacheDir: File = File(context.cacheDir, "video_cache")
    private val maxCacheSize = 100L * 1024 * 1024 // 100 MB max cache size

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    companion object {
        @Volatile
        private var instance: VideoCacheManager? = null

        fun getInstance(context: Context): VideoCacheManager {
            return instance ?: synchronized(this) {
                instance ?: VideoCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /** Returns cached video or downloads it if not cached. */
    suspend fun getCachedVideo(videoUrl: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = generateFileName(videoUrl)
            val cacheFile = File(cacheDir, fileName)

            if (cacheFile.exists()) {
                // Update last modified time for LRU cleanup
                cacheFile.setLastModified(System.currentTimeMillis())
                Log.d("VideoCacheManager", "Cache HIT: $fileName")
                return@withContext Uri.fromFile(cacheFile)
            }

            // Download video to cache
            Log.d("VideoCacheManager", "Cache MISS: Downloading $fileName")
            downloadVideo(videoUrl, cacheFile)

            // Cleanup old files if cache is too large
            cleanupCache()

            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.e("VideoCacheManager", "Error caching video", e)
            // Return original URL if caching fails
            Uri.parse(videoUrl)
        }
    }

    /** Checks if video is already cached. */
    fun isCached(videoUrl: String): Boolean {
        val fileName = generateFileName(videoUrl)
        return File(cacheDir, fileName).exists()
    }

    /** Returns cached file if it exists. */
    fun getCachedFileIfExists(videoUrl: String): File? {
        val fileName = generateFileName(videoUrl)
        val file = File(cacheDir, fileName)
        return if (file.exists()) file else null
    }

    /** Downloads video from URL to local file. */
    private fun downloadVideo(videoUrl: String, destFile: File) {
        URL(videoUrl).openStream().use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    /** Generates unique filename from URL using MD5 hash. */
    private fun generateFileName(url: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(url.toByteArray())
        val hexString = digest.joinToString("") { "%02x".format(it) }
        
        // Extract file extension from URL
        val extension = url.substringAfterLast('.', "mp4").take(4)
        
        return "$hexString.$extension"
    }

    /** Removes old cached files using LRU strategy. */
    private fun cleanupCache() {
        val files = cacheDir.listFiles() ?: return
        val totalSize = files.sumOf { it.length() }

        if (totalSize > maxCacheSize) {
            // Sort by last modified time (oldest first)
            val sortedFiles = files.sortedBy { it.lastModified() }
            var currentSize = totalSize

            for (file in sortedFiles) {
                if (currentSize <= maxCacheSize * 0.8) break // Keep 80% of max size
                
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                    Log.d("VideoCacheManager", "Deleted old cache file: ${file.name}")
                }
            }
        }
    }

    /** Clears all cached videos. */
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        Log.d("VideoCacheManager", "Cleared all video cache")
    }

    /** Returns cache statistics. */
    fun getCacheStats(): String {
        val files = cacheDir.listFiles() ?: emptyArray()
        val totalSize = files.sumOf { it.length() }
        val sizeMB = totalSize / (1024.0 * 1024.0)
        return "Video Cache: ${files.size} files, %.2f MB / %.0f MB".format(sizeMB, maxCacheSize / (1024.0 * 1024.0))
    }
}
