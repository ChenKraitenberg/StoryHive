package com.example.storyhive.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.storyhive.data.local.dao.BookDao
import com.example.storyhive.data.local.dao.CommentDao
import com.example.storyhive.data.local.dao.ImageCacheDao
import com.example.storyhive.data.local.dao.PostDao
import com.example.storyhive.data.local.dao.UserDao
import com.example.storyhive.data.local.entities.BookEntity
import com.example.storyhive.data.local.entities.CommentEntity
import com.example.storyhive.data.local.entities.ImageCacheEntity
import com.example.storyhive.data.local.entities.PostEntity
import com.example.storyhive.data.local.entities.UserEntity
import com.example.storyhive.data.local.util.Converters

/**
 * Room database for storing application data locally.
 * This database includes tables for books, posts, comments, users, and image caching.
 */
@Database(
    entities = [
        BookEntity::class,
        PostEntity::class,
        CommentEntity::class,
        UserEntity::class,
        ImageCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StoryHiveDatabase : RoomDatabase() {

    //Data Access Object (DAO) for books, posts, comments, users, cached images
    abstract fun bookDao(): BookDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun imageCacheDao(): ImageCacheDao

    companion object {
        @Volatile
        private var INSTANCE: StoryHiveDatabase? = null

        /**
         * Returns a singleton instance of the database.
         * Uses synchronized block to ensure only one instance is created.
         */
        fun getInstance(context: Context): StoryHiveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoryHiveDatabase::class.java,
                    "storyhive_database"
                )
                    .fallbackToDestructiveMigration() // Allows destructive migration if needed
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}