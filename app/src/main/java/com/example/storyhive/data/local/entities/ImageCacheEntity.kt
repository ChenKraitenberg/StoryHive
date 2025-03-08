package com.example.storyhive.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_cache")
data class ImageCacheEntity(
    @PrimaryKey
    val url: String,
    val localPath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val size: Long = 0,
    val mimeType: String = "image/jpeg"
)