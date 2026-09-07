package com.example.data

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * Provides a production-grade Coil ImageLoader with:
 * - 25% of available RAM for memory cache
 * - 100MB disk cache
 * - Automatic downscaling to target size
 * - Aggressive caching policies
 */
object AuraImageLoader {

    @Volatile
    private var INSTANCE: ImageLoader? = null

    fun getInstance(context: Context): ImageLoader {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildImageLoader(context).also { INSTANCE = it }
        }
    }

    private fun buildImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% of available RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("aura_image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)           // Smooth fade-in transitions
            .crossfade(300)            // 300ms crossfade duration
            .respectCacheHeaders(false) // Always cache regardless of headers
            .build()
    }
}
