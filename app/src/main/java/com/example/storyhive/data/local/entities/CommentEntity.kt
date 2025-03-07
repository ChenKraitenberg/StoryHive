package com.example.storyhive.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.storyhive.data.models.Comment
import java.util.UUID

/**
 * Represents a comment entity stored in the local database.
 */
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val commentId: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = false
)

// Converts a Room entity to a domain model
fun CommentEntity.toDomainModel(): Comment {
    return Comment(
        commentId = commentId,
        userId = userId,
        userName = userName,
        content = content,
        timestamp = timestamp
    )
}

// Converts a domain model to a Room entity
fun Comment.toEntity(postId: String = "", pendingSync: Boolean = false): CommentEntity {
    return CommentEntity(
        commentId = commentId,
        postId = postId,
        userId = userId,
        userName = userName,
        content = content,
        timestamp = timestamp,
        pendingSync = pendingSync
    )
}