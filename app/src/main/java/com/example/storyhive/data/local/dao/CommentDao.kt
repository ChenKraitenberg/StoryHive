package com.example.storyhive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.storyhive.data.local.entities.CommentEntity

import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE commentId = :commentId")
    suspend fun getCommentById(commentId: String): CommentEntity?

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp DESC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE commentId = :commentId")
    suspend fun deleteComment(commentId: String)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: String)

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    suspend fun getCommentCount(postId: String): Int

    @Query("SELECT * FROM comments WHERE pendingSync = 1")
    suspend fun getPendingComments(): List<CommentEntity>

    @Query("UPDATE comments SET pendingSync = :pending WHERE commentId = :commentId")
    suspend fun updateSyncStatus(commentId: String, pending: Boolean)

    @Query("SELECT * FROM comments WHERE userId = :userId")
    fun getUserComments(userId: String): Flow<List<CommentEntity>>
}