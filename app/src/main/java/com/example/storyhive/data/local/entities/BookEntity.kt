package com.example.storyhive.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.storyhive.data.models.Book


/**
 * Represents a book entity stored in the local database.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val description: String = "",
    val coverUrl: String = "",
    val genre: String,
    val rating: Float,
    val pageCount: Int,
    val publishedDate: String,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        // Extension functions for converting between entity and domain model

        /**
         * Converts a BookEntity object to a Book domain model.
         */
        fun BookEntity.toDomainModel(): Book {
            return Book(
                id = id,
                title = title,
                author = author,
                description = description,
                coverUrl = coverUrl,
                genre = genre,
                rating = rating,
                pageCount = pageCount,
                publishedDate = publishedDate
            )
        }

        /**
         * Converts a Book domain model to a BookEntity object for database storage.
         */
        fun Book.toEntity(): BookEntity {
            return BookEntity(
                id = id,
                title = title,
                author = author,
                description = description,
                coverUrl = coverUrl,
                genre = genre,
                rating = rating,
                pageCount = pageCount,
                publishedDate = publishedDate
            )
        }
    }
}