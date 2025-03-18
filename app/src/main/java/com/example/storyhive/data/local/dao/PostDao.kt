package com.example.storyhive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.storyhive.data.local.entities.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUser(userId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE postId = :postId")
    fun getPostById(postId: String): Flow<PostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE postId = :postId")
    suspend fun deletePost(postId: String)

    @Query("SELECT * FROM posts WHERE pendingSync = 1")
    suspend fun getPendingPosts(): List<PostEntity>

    @Query("UPDATE posts SET pendingSync = :pending WHERE postId = :postId")
    suspend fun updateSyncStatus(postId: String, pending: Boolean)

    @Query("UPDATE posts SET likes = :likes, likedBy = :likedBy WHERE postId = :postId")
    suspend fun updateLikes(postId: String, likes: Int, likedBy: List<String>)

    @Query("UPDATE posts SET commentCount = :commentCount WHERE postId = :postId")
    suspend fun updateCommentCount(postId: String, commentCount: Int)

    @Query("SELECT * FROM posts WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getPostsByBook(bookId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE bookTitle LIKE '%' || :query || '%' OR bookAuthor LIKE '%' || :query || '%' OR review LIKE '%' || :query || '%'")
    fun searchPosts(query: String): Flow<List<PostEntity>>

    @Query("UPDATE posts SET userDisplayName = :newName WHERE userId = :userId")
    suspend fun updateUserDisplayName(userId: String, newName: String)
}