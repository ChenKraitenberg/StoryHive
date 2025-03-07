package com.example.storyhive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.storyhive.data.local.entities.ImageCacheEntity

@Dao
interface ImageCacheDao {
    @Query("SELECT * FROM image_cache WHERE url = :url")
    suspend fun getImageByUrl(url: String): ImageCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ImageCacheEntity>)

    @Query("DELETE FROM image_cache WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM image_cache WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM image_cache")
    suspend fun getCount(): Int

    @Query("SELECT SUM(size) FROM image_cache")
    suspend fun getTotalSize(): Long

    @Query("SELECT * FROM image_cache ORDER BY timestamp")
    suspend fun getAllOrderedByOldest(): List<ImageCacheEntity>

    @Query("SELECT * FROM image_cache")
    suspend fun getAllImageUrls(): List<ImageCacheEntity>

    @Query("DELETE FROM image_cache")
    suspend fun clearAll()
}