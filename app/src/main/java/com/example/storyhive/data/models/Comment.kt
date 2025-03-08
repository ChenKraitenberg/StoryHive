package com.example.storyhive.data.models

import java.util.UUID

data class Comment(
    val commentId: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)