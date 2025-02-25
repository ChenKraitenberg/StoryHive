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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // הוספת תגובה
    suspend fun addComment(postId: String, content: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Anonymous"

        val comment = Comment(
            userId = userId,
            userName = userName,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        postsCollection
            .document(postId)
            .collection("comments")
            .add(comment)
    }

    // הבאת תגובות לפוסט
    fun observeComments(postId: String, onCommentsUpdated: (List<Comment>) -> Unit) {
        postsCollection
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)
                } ?: emptyList()

                onCommentsUpdated(comments)
            }
    }

    // יצירת פוסט חדש
//    suspend fun createPost(post: Post) {
//        val user = FirebaseAuth.getInstance().currentUser
//        if (user != null) {
//            val newPostRef = postsCollection.document()
//            val postWithUserInfo = post.copy(
//                postId = newPostRef.id,
//                userId = user.uid,
//                userDisplayName = user.displayName ?: "Unknown",  // הוספת שם המשתמש
//                timestamp = System.currentTimeMillis()
//            )
//            newPostRef.set(postWithUserInfo).await()
//        }
//    }

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


}