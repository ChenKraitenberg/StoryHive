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

