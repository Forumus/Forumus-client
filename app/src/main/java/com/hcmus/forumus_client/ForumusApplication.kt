package com.hcmus.forumus_client

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Custom Application class with optimized image and video caching configuration.
 */
class ForumusApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * Create a custom ImageLoader with aggressive caching for better performance.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Use 25% of available memory for image cache
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // 250 MB disk cache for images
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            // Enable disk and memory caching
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // Enable network caching
            .networkCachePolicy(CachePolicy.ENABLED)
            // Allow hardware bitmaps for better performance
            .allowHardware(true)
            // Crossfade for smooth transitions
            .crossfade(true)
            // Uncomment for debugging cache hits/misses
            // .logger(DebugLogger())
            .build()
    }
}
