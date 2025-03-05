package com.example.storyhive.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    val postId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userProfileImage: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookDescription: String = "",
    var imageBase64: String? = null, // יכול להיות null
    var imageUrl: String? = null, // יכול להיות null
    var rating: Float = 0.0f,
    val review: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val commentCount: Int = 0
) : Parcelable

{
    // הוסף המרה לMap עבור Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "postId" to postId,
        "userId" to userId,
        "userDisplayName" to userDisplayName,
        "userProfileImage" to userProfileImage,
        "bookTitle" to bookTitle,
        "bookAuthor" to bookAuthor,
        "bookDescription" to bookDescription,
        "review" to review,
        "imageBase64" to imageBase64,
        "imageUrl" to imageUrl,
        "timestamp" to timestamp,
        "likes" to likes,
        "likedBy" to likedBy,
        "commentCount" to commentCount
    )
}

