package com.example.storyhive.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.storyhive.data.models.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val postId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userProfileImage: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookDescription: String = "",
    val imageBase64: String? = null,
    val imageUrl: String? = null,
    val rating: Float = 0.0f,
    val review: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val commentCount: Int = 0,
    val pendingSync: Boolean = false
)

// Converts a Room entity to a domain model
fun PostEntity.toDomainModel(): Post {
    return Post(
        postId = postId,
        userId = userId,
        userDisplayName = userDisplayName,
        userProfileImage = userProfileImage,
        bookId = bookId,
        bookTitle = bookTitle,
        bookAuthor = bookAuthor,
        bookDescription = bookDescription,
        imageBase64 = imageBase64,
        imageUrl = imageUrl,
        rating = rating,
        review = review,
        likes = likes,
        likedBy = likedBy,
        timestamp = timestamp,
        commentCount = commentCount
    )
}

// Converts a domain model to a Room entity
fun Post.toEntity(pendingSync: Boolean = false): PostEntity {
    return PostEntity(
        postId = postId,
        userId = userId,
        userDisplayName = userDisplayName,
        userProfileImage = userProfileImage,
        bookId = bookId,
        bookTitle = bookTitle,
        bookAuthor = bookAuthor,
        bookDescription = bookDescription,
        imageBase64 = imageBase64,
        imageUrl = imageUrl,
        rating = rating,
        review = review,
        likes = likes,
        likedBy = likedBy,
        timestamp = timestamp,
        commentCount = commentCount,
        pendingSync = pendingSync
    )
}