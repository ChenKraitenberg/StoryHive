package com.example.storyhive.data.local

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.storyhive.data.local.dao.ImageCacheDao
import com.example.storyhive.data.local.entities.ImageCacheEntity
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ImageCacheManager private constructor(
    private val context: Context,
    private val imageCacheDao: ImageCacheDao
) {
    private val cacheDir = File(context.cacheDir, "image_cache").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Receives an image URL, downloads it, and stores it in the local cache.
     * Returns the local path to the image.
     */
    suspend fun cacheImage(url: String): String? = withContext(Dispatchers.IO) {
        try {
            // Check if the image already exists in cache
            val existingCache = imageCacheDao.getImageByUrl(url)
            if (existingCache != null) {
                val file = File(existingCache.localPath)
                if (file.exists()) {
                    // Update timestamp
                    imageCacheDao.insert(existingCache.copy(timestamp = System.currentTimeMillis()))
                    return@withContext existingCache.localPath
                } else {
                    // File was deleted from the device, remove from database
                    imageCacheDao.deleteByUrl(url)
                }
            }

            // Create a unique filename based on the URL
            val fileName = "${url.hashCode()}.jpg"
            val file = File(cacheDir, fileName)

            // Make sure directory exists
            if (!file.parentFile?.exists()!!) {
                file.parentFile?.mkdirs()
            }

            // Download the image using Picasso
            try {
                val bitmap = Picasso.get().load(url).get()

                // Save the image to file
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }

                // Calculate file size
                val fileSize = file.length()

                // If file size is 0, something went wrong
                if (fileSize == 0L) {
                    file.delete()
                    throw IOException("Failed to write image data")
                }

                // Save info to database
                val localPath = file.absolutePath
                val cacheEntity = ImageCacheEntity(
                    url = url,
                    localPath = localPath,
                    timestamp = System.currentTimeMillis(),
                    size = fileSize
                )
                imageCacheDao.insert(cacheEntity)

                return@withContext localPath
            } catch (e: Exception) {
                // Clean up on error
                if (file.exists()) {
                    file.delete()
                }
                throw e
            }
        } catch (e: Exception) {
            Log.e("ImageCacheManager", "Failed to cache image: $url", e)
            null
        }
    }

    /**
     * Receives a URL and returns the local path if the image exists in the cache.
     */
    suspend fun getLocalPathForUrl(url: String): String? = withContext(Dispatchers.IO) {
        val cache = imageCacheDao.getImageByUrl(url)
        if (cache != null) {
            val file = File(cache.localPath)
            if (file.exists()) {
                // Update timestamp
                imageCacheDao.insert(cache.copy(timestamp = System.currentTimeMillis()))
                return@withContext cache.localPath
            } else {
                // File was deleted from the device, remove it from the database
                imageCacheDao.deleteByUrl(url)
            }
        }
        null
    }

    /**
     * Cleans up old images from the cache.
     */
    suspend fun cleanupCache(maxAgeDays: Int = 7) = withContext(Dispatchers.IO) {
        try {
            // Delete old images from the database
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxAgeDays.toLong())
            imageCacheDao.deleteOlderThan(cutoffTime)

            // Delete files that no longer have a record in the database
            val cachedImages = imageCacheDao.getAllImageUrls()
            val cachedPaths = cachedImages.map { it.localPath }

            cacheDir.listFiles()?.forEach { file ->
                if (!cachedPaths.contains(file.absolutePath)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("ImageCacheManager", "Failed to clean up cache", e)
        }
    }

    /**
     * Clears all cached images.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            // Delete all records from the database
            imageCacheDao.clearAll()

            // Delete all files from the cache directory
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e("ImageCacheManager", "Failed to clear cache", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ImageCacheManager? = null


        /**
         * Returns a singleton instance of ImageCacheManager.
         */
        fun getInstance(context: Context, imageCacheDao: ImageCacheDao): ImageCacheManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ImageCacheManager(context, imageCacheDao)
                INSTANCE = instance
                instance
            }
        }
    }
}