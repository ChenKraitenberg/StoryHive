package com.example.storyhive.data.models

data class Review(
    val userId: String,
    val bookId: String,
    val userName: String,
    val content: String,
    val rating: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val userImageUrl: String? = null
)
