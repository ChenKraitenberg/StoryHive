package com.example.storyhive.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.storyhive.data.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.example.storyhive.data.models.Comment
import com.example.storyhive.data.models.UserPostsStats
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postsCollection = firestore.collection("posts")

    // מביא את כל הפוסטים (למסך הבית)
    fun observePosts(onPostsUpdated: (List<Post>) -> Unit) {
        postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()

                // עדכון הנתונים ב- Main Thread
                Handler(Looper.getMainLooper()).post {
                    onPostsUpdated(posts)
                }
            }
    }



    // מביא רק את הפוסטים של המשתמש הספציפי (למסך פרופיל)
    fun observeUserPosts(userId: String, onPostsUpdated: (List<Post>) -> Unit) {
        postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()

                onPostsUpdated(posts)
            }
    }

    // טיפול בלייקים
    suspend fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = postsCollection.document(postId)

        firestore.runTransaction { transaction ->
            val post = transaction.get(postRef).toObject(Post::class.java)
                ?: throw Exception("Post not found")

            val newLikedBy = if (userId in post.likedBy) {
                post.likedBy - userId
            } else {
                post.likedBy + userId
            }

            transaction.update(postRef,
                mapOf(
                    "likedBy" to newLikedBy,
                    "likes" to newLikedBy.size
                )
            )
        }
    }


    suspend fun createPost(post: Post) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                // נסה לקבל את תמונת הפרופיל מ-Firestore
                val userDocument = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                // קבל את תמונת הפרופיל כ-base64 אם קיימת
                var userProfileImage = ""
                if (userDocument.exists()) {
                    userProfileImage = userDocument.getString("profileImageBase64") ?: ""
                    Log.d("FirebaseRepository", "Found user profile image: ${userProfileImage.take(20)}...")
                }

                // יצירת הפוסט עם תמונת הפרופיל
                val newPostRef = postsCollection.document()
                val postWithUserInfo = post.copy(
                    postId = newPostRef.id,
                    userId = user.uid,
                    userDisplayName = user.displayName ?: "Unknown",
                    userProfileImage = userProfileImage,  // שימוש בתמונה שמצאנו
                    timestamp = System.currentTimeMillis()
                )

                Log.d("FirebaseRepository", "Creating post with profileImage included: ${userProfileImage.isNotEmpty()}")
                newPostRef.set(postWithUserInfo).await()
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error creating post", e)
                throw e
            }
        }
    }


     fun likePost(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = postsCollection.document(postId)

        firestore.runTransaction { transaction ->
            val post = transaction.get(postRef).toObject(Post::class.java)
                ?: throw Exception("Post not found")

            val newLikedBy = if (userId in post.likedBy) {
                post.likedBy - userId
            } else {
                post.likedBy + userId
            }

            transaction.update(postRef,
                mapOf(
                    "likedBy" to newLikedBy,
                    "likes" to newLikedBy.size
                )
            )
        }
    }

    // getUserPostsStats
    fun getUserPostsStats(userId: String, onStatsUpdated: (UserPostsStats) -> Unit) {
        postsCollection
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val postsCount = snapshot.size()
                val totalLikes = snapshot.documents.sumOf { (it.get("likes") as? Long ?: 0).toInt() }
                onStatsUpdated(UserPostsStats(postsCount, totalLikes))
            }
    }

    //delete post
    suspend fun deletePost(postId: String): Boolean {
        return try {
            firestore.collection("posts").document(postId).delete().await()
            true // ✅ החזרת true אם המחיקה הצליחה
        } catch (e: Exception) {
            Log.e("PostRepository", "שגיאה במחיקת הפוסט: ${e.message}")
            false // ✅ החזרת false אם המחיקה נכשלה
        }
    }


    suspend fun addComment(postId: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid
                    ?: throw Exception("User not logged in")
                val userName = auth.currentUser?.displayName ?: "Anonymous"

                // יצירת מזהה ייחודי לתגובה
                val commentId = UUID.randomUUID().toString()

                val comment = Comment(
                    commentId = commentId,
                    userId = userId,
                    userName = userName,
                    content = content.trim(),
                    timestamp = System.currentTimeMillis()
                )

                // בדיקת תקינות תוכן התגובה
                if (content.trim().isEmpty()) {
                    throw IllegalArgumentException("Comment cannot be empty")
                }

                // שלב 1: שמירת התגובה
                postsCollection
                    .document(postId)
                    .collection("comments")
                    .document(commentId)
                    .set(comment)
                    .await()

                Log.d("PostRepository", "Comment saved with ID: $commentId")

                // שלב 2: עדכון מספר התגובות בפוסט
                updateCommentCount(postId)

                Log.d("PostRepository", "Comment count updated for post $postId")
            } catch (e: Exception) {
                Log.e("PostRepository", "Error adding comment: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Boolean {
        return try {
            // First check if the current user is the comment creator
            val userId = auth.currentUser?.uid ?: return false
            val commentDoc = postsCollection
                .document(postId)
                .collection("comments")
                .document(commentId)
                .get()
                .await()

            val comment = commentDoc.toObject(Comment::class.java)

            // Only allow deletion if the user created the comment
            if (comment?.userId == userId) {
                postsCollection
                    .document(postId)
                    .collection("comments")
                    .document(commentId)
                    .delete()
                    .await()

                // Update comment count
                updateCommentCount(postId)
                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting comment: ${e.message}")
            return false
        }
    }

    // עדכון מספר התגובות בפוסט
    private suspend fun updateCommentCount(postId: String) {
        try {
            // Get the snapshot of comments collection
            val commentsSnapshot = postsCollection
                .document(postId)
                .collection("comments")
                .get()
                .await()

            // Count the documents in the snapshot
            val commentCount = commentsSnapshot.size()

            // Update the post document with the new count
            postsCollection
                .document(postId)
                .update("commentCount", commentCount)
                .await()

            Log.d("PostRepository", "Updated comment count for post $postId: $commentCount")
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating comment count: ${e.message}")
            throw e
        }
    }

// הבאת תגובות לפוסט
    fun observeComments(postId: String, onCommentsUpdated: (List<Comment>) -> Unit) {
        postsCollection
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostRepository", "Error observing comments: ${error.message}")
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    val comment = doc.toObject(Comment::class.java)
                    // If the comment doesn't have an ID (old data), use the document ID
                    comment?.copy(commentId = comment.commentId.ifEmpty { doc.id })
                } ?: emptyList()

                // Deliver on main thread to ensure UI safety
                Handler(Looper.getMainLooper()).post {
                    onCommentsUpdated(comments)
                }
            }
    }

    // Get comment count for a post
    suspend fun getCommentCount(postId: String): Long {
        return try {
            val commentsSnapshot = postsCollection
                .document(postId)
                .collection("comments")
                .get()
                .await()

            commentsSnapshot.size().toLong()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error getting comment count: ${e.message}")
            0L
        }
    }

    // In PostRepository.kt, add this method:
    fun getPostsByBookId(bookId: String, onPostsUpdated: (List<Post>) -> Unit) {
        postsCollection
            .whereEqualTo("bookId", bookId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()

                onPostsUpdated(posts)
            }
    }

}