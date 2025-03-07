package com.example.storyhive.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.storyhive.data.local.dao.PostDao
import com.example.storyhive.data.local.dao.CommentDao
import com.example.storyhive.data.local.entities.toDomainModel
import com.example.storyhive.data.local.entities.toEntity
import com.example.storyhive.data.models.Post
import com.example.storyhive.data.models.Comment
import com.example.storyhive.data.models.UserPostsStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postsCollection = firestore.collection("posts")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Local database repositories to be initialized
    private var postDao: PostDao? = null
    private var commentDao: CommentDao? = null
    private var isOfflineModeInitialized = false

    companion object {
        @Volatile
        private var INSTANCE: PostRepository? = null

        // Retrieves a singleton instance of PostRepository
        fun getInstance(): PostRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PostRepository()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Initializes offline mode by setting up local database DAOs.
     * This method should be called from the Application class.
     */
    fun initOfflineMode(postDao: PostDao, commentDao: CommentDao) {
        this.postDao = postDao
        this.commentDao = commentDao
        isOfflineModeInitialized = true


        // Start background sync when online
        coroutineScope.launch {
            try {
                // Listen for network changes
                if (isNetworkAvailable()) {
                    syncPendingData()
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Error during init", e)
            }
        }

        Log.d("PostRepository", "Offline mode initialized with DAOs")
    }


    /**
     * Checks if network connectivity is available.
     * Replace with actual implementation using a connectivity manager.
     */
    private fun isNetworkAvailable(): Boolean {
        return true // Placeholder
    }

    // Checks if offline mode is supported and initialized
    private fun isOfflineSupported(): Boolean {
        return isOfflineModeInitialized && postDao != null && commentDao != null
    }

    // Retrieves all posts and observes real-time updates (Home Screen)
    fun observePosts(onPostsUpdated: (List<Post>) -> Unit) {
        // Load from local database if offline mode is initialized
        if (isOfflineSupported()) {
            coroutineScope.launch {
                try {
                    val localPosts = postDao!!.getAllPosts().first()
                    withContext(Dispatchers.Main) {
                        onPostsUpdated(localPosts.map { it.toDomainModel() })
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error loading local posts", e)
                }
            }
        }

        // Listen for real-time updates from Firestore
        postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostRepository", "Error listening to posts", error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()

                // Save locally if offline mode is enabled
                if (isOfflineSupported()) {
                    coroutineScope.launch {
                        try {
                            postDao!!.insertPosts(posts.map { it.toEntity() })
                        } catch (e: Exception) {
                            Log.e("PostRepository", "Error saving posts locally", e)
                        }
                    }
                }

                // Update data on the main thread
                Handler(Looper.getMainLooper()).post {
                    onPostsUpdated(posts)
                }
            }
    }

    // Retrieves posts by a specific user and observes updates (Profile Screen)
    fun observeUserPosts(userId: String, onPostsUpdated: (List<Post>) -> Unit) {
        if (isOfflineSupported()) {
            coroutineScope.launch {
                try {
                    val localPosts = postDao!!.getPostsByUser(userId).first()
                    withContext(Dispatchers.Main) {
                        onPostsUpdated(localPosts.map { it.toDomainModel() })
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error loading local user posts", e)
                }
            }
        }

        postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostRepository", "Error listening to user posts", error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()

                // save local
                if (isOfflineSupported()) {
                    coroutineScope.launch {
                        try {
                            postDao!!.insertPosts(posts.map { it.toEntity() })
                        } catch (e: Exception) {
                            Log.e("PostRepository", "Error saving user posts locally", e)
                        }
                    }
                }

                onPostsUpdated(posts)
            }
    }

    /**
     * Toggles the like status for a post.
     * Updates both local and remote databases if offline mode is enabled.
     */
    suspend fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = postsCollection.document(postId)

        try {
            if (isOfflineSupported()) {
                val localPost = postDao!!.getPostById(postId).first()
                if (localPost != null) {
                    val post = localPost.toDomainModel()
                    val newLikedBy = if (userId in post.likedBy) post.likedBy - userId else post.likedBy + userId
                    postDao!!.updateLikes(postId, newLikedBy.size, newLikedBy)
                }
            }

            // update on server
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
            }.await()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error toggling like", e)
        }
    }

    suspend fun createPost(post: Post) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                // Try to retrieve the user's profile image from Firestore
                val userDocument = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                // Get the profile image as base64 if it exists
                var userProfileImage = ""
                if (userDocument.exists()) {
                    userProfileImage = userDocument.getString("profileImageBase64") ?: ""
                    Log.d("PostRepository", "Found user profile image: ${userProfileImage.take(20)}...")
                }

                // Create the post including the user's profile image
                val newPostRef = postsCollection.document()
                val postWithUserInfo = post.copy(
                    postId = newPostRef.id,
                    userId = user.uid,
                    userDisplayName = user.displayName ?: "Unknown",
                    userProfileImage = userProfileImage,  // Using the retrieved profile image
                    timestamp = System.currentTimeMillis()
                )

                Log.d("PostRepository", "Creating post with profileImage included: ${userProfileImage.isNotEmpty()}")

                // Save the post to Firestore
                newPostRef.set(postWithUserInfo).await()

                // Save locally if offline mode is initialized
                if (isOfflineSupported()) {
                    postDao!!.insertPost(postWithUserInfo.toEntity())
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Error creating post", e)

                // Try to save locally if the operation fails
                if (isOfflineSupported() && post.postId.isNotEmpty()) {
                    try {
                        postDao!!.insertPost(post.toEntity(pendingSync = true))
                        Log.d("PostRepository", "Saved post locally for later sync")
                    } catch (ex: Exception) {
                        Log.e("PostRepository", "Failed to save post locally", ex)
                    }
                }

                throw e
            }
        }
    }

    fun likePost(postId: String) {
        val userId = auth.currentUser?.uid ?: return

        // If offline mode is enabled, update locally first
        if (isOfflineSupported()) {
            coroutineScope.launch {
                try {
                    val localPost = postDao!!.getPostById(postId).first()
                    if (localPost != null) {
                        val post = localPost.toDomainModel()
                        val newLikedBy = if (userId in post.likedBy) post.likedBy - userId else post.likedBy + userId
                        postDao!!.updateLikes(postId, newLikedBy.size, newLikedBy)
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error updating local like", e)
                }
            }
        }

        // Update the like status in Firestore
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

    // Retrieves the statistics of a user's posts (number of posts and total likes)
    fun getUserPostsStats(userId: String, onStatsUpdated: (UserPostsStats) -> Unit) {
        if (isOfflineSupported()) {
            coroutineScope.launch {
                try {
                    val localPosts = postDao!!.getPostsByUser(userId).first()
                    val localStats = UserPostsStats(
                        postsCount = localPosts.size,
                        totalLikes = localPosts.sumOf { it.likes }
                    )
                    withContext(Dispatchers.Main) {
                        onStatsUpdated(localStats)
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error calculating local stats", e)
                }
            }
        }

        // uploads statistics from server
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
            // Attempt to delete from Firestore
            firestore.collection("posts").document(postId).delete().await()

            // Delete locally if offline mode is enabled
            if (isOfflineSupported()) {
                postDao!!.deletePost(postId)
                commentDao!!.deleteCommentsForPost(postId)
            }

            true
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting post: ${e.message}")

            // Attempt to delete locally even if Firestore deletion fails
            if (isOfflineSupported()) {
                try {
                    postDao!!.deletePost(postId)
                    commentDao!!.deleteCommentsForPost(postId)
                } catch (ex: Exception) {
                    Log.e("PostRepository", "Error deleting post locally", ex)
                }
            }

            false
        }
    }

    //Add comment
    suspend fun addComment(postId: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid
                    ?: throw Exception("User not logged in")
                val userName = auth.currentUser?.displayName ?: "Anonymous"

                // Generate a unique ID for the comment
                val commentId = UUID.randomUUID().toString()

                val comment = Comment(
                    commentId = commentId,
                    postId = postId,
                    userId = userId,
                    userName = userName,
                    content = content.trim(),
                    timestamp = System.currentTimeMillis()
                )

                // Validate comment content
                if (content.trim().isEmpty()) {
                    throw IllegalArgumentException("Comment cannot be empty")
                }

                // Step 1: Save the comment to Firestore
                postsCollection
                    .document(postId)
                    .collection("comments")
                    .document(commentId)
                    .set(comment)
                    .await()

                Log.d("PostRepository", "Comment saved with ID: $commentId")

                // Step 2: Update the comment count in the post
                updateCommentCount(postId)

                // Save locally if offline mode is enabled
                if (isOfflineSupported()) {
                    commentDao!!.insertComment(comment.toEntity(postId))

                    // Update comment count in the local database
                    val count = commentDao!!.getCommentCount(postId)
                    postDao!!.updateCommentCount(postId, count)
                }

                Log.d("PostRepository", "Comment count updated for post $postId")
            } catch (e: Exception) {
                Log.e("PostRepository", "Error adding comment: ${e.message}", e)

                // Try saving locally if the operation fails and offline mode is enabled
                if (isOfflineSupported()) {
                    try {
                        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                        val userName = auth.currentUser?.displayName ?: "Anonymous"
                        val commentId = UUID.randomUUID().toString()

                        val comment = Comment(
                            commentId = commentId,
                            postId = postId,
                            userId = userId,
                            userName = userName,
                            content = content.trim(),
                            timestamp = System.currentTimeMillis(),
                        )

                        commentDao!!.insertComment(comment.toEntity(postId, pendingSync = true))

                        // Update comment count in the local database
                        val count = commentDao!!.getCommentCount(postId)
                        postDao!!.updateCommentCount(postId, count)

                        Log.d("PostRepository", "Comment saved locally for later sync")
                    } catch (ex: Exception) {
                        Log.e("PostRepository", "Failed to save comment locally", ex)
                    }
                }

                throw e
            }
        }
    }

    //Delete comment
    suspend fun deleteComment(postId: String, commentId: String): Boolean {
        return try {
            // First check if the current user is the comment creator
            val userId = auth.currentUser?.uid ?: return false

            // Check if the comment exists locally if offline mode is enabled
            var commentUserId = ""
            if (isOfflineSupported()) {
                val localComment = commentDao!!.getCommentById(commentId)
                if (localComment != null) {
                    commentUserId = localComment.userId
                }
            }

            // If the comment was not found locally, check Firestore
            if (commentUserId.isEmpty()) {
                val commentDoc = postsCollection
                    .document(postId)
                    .collection("comments")
                    .document(commentId)
                    .get()
                    .await()

                val comment = commentDoc.toObject(Comment::class.java)
                if (comment != null) {
                    commentUserId = comment.userId
                }
            }

            // Allow deletion only if the user is the creator of the comment
            if (commentUserId == userId) {
                // Delete from Firestore
                try {
                    postsCollection
                        .document(postId)
                        .collection("comments")
                        .document(commentId)
                        .delete()
                        .await()

                    // Update comment count on server
                    updateCommentCount(postId)
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error deleting comment from server", e)
                }

                // Delete locally if offline mode is enabled
                if (isOfflineSupported()) {
                    commentDao!!.deleteComment(commentId)

                    // Update the comment count in the local database
                    val count = commentDao!!.getCommentCount(postId)
                    postDao!!.updateCommentCount(postId, count)
                }

                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting comment: ${e.message}")
            return false
        }
    }

    // Update the comment count for a post
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

            // Update locally if offline mode is enabled
            if (isOfflineSupported()) {
                postDao!!.updateCommentCount(postId, commentCount)
            }

            Log.d("PostRepository", "Updated comment count for post $postId: $commentCount")
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating comment count: ${e.message}")
            throw e
        }
    }

    // Retrieve comments for a post
    fun observeComments(postId: String, onCommentsUpdated: (List<Comment>) -> Unit) {
        // Load local comments if offline mode is enabled
        if (isOfflineSupported()) {
            coroutineScope.launch {
                try {
                    val localComments = commentDao!!.getCommentsForPost(postId).first()
                    withContext(Dispatchers.Main) {
                        onCommentsUpdated(localComments.map { it.toDomainModel() })
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error loading local comments", e)
                }
            }
        }

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

                // Save locally if offline mode is enabled
                if (isOfflineSupported()) {
                    coroutineScope.launch {
                        try {
                            commentDao!!.insertComments(comments.map { it.toEntity(postId) })
                        } catch (e: Exception) {
                            Log.e("PostRepository", "Error saving comments locally", e)
                        }
                    }
                }

                // Deliver on main thread to ensure UI safety
                Handler(Looper.getMainLooper()).post {
                    onCommentsUpdated(comments)
                }
            }
    }

    // Get comment count for a post
    suspend fun getCommentCount(postId: String): Long {
        return try {
            // Try retrieving from local storage first if offline mode is enabled
            if (isOfflineSupported()) {
                val localCount = commentDao!!.getCommentCount(postId)
                if (localCount > 0) {
                    return localCount.toLong()
                }
            }

            // Otherwise, or in addition, fetch from Firestore
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


    //Get posts by book id
    fun getPostsByBookId(bookId: String, onPostsUpdated: (List<Post>) -> Unit) {
        // Load posts from local storage if offline mode is enabled
        if (isOfflineSupported()) {
            coroutineScope.launch {
                try {
                    val localPosts = postDao!!.getPostsByBook(bookId).first()
                    withContext(Dispatchers.Main) {
                        onPostsUpdated(localPosts.map { it.toDomainModel() })
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error loading local book posts", e)
                }
            }
        }

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

                // Save locally if offline mode is enabled
                if (isOfflineSupported()) {
                    coroutineScope.launch {
                        try {
                            postDao!!.insertPosts(posts.map { it.toEntity() })
                        } catch (e: Exception) {
                            Log.e("PostRepository", "Error saving book posts locally", e)
                        }
                    }
                }

                onPostsUpdated(posts)
            }
    }

    // Update post
    suspend fun updatePost(post: Post) {
        try {
            // Attempt to update on the server
            postsCollection.document(post.postId).set(post).await()

            // Update locally if offline mode is enabled
            if (isOfflineSupported()) {
                postDao!!.insertPost(post.toEntity())
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating post", e)

            // Try saving locally if the update fails
            if (isOfflineSupported()) {
                try {
                    postDao!!.insertPost(post.toEntity(pendingSync = true))
                } catch (ex: Exception) {
                    Log.e("PostRepository", "Failed to save updated post locally", ex)
                }
            }

            throw e
        }
    }

    // Synchronize pending data
    suspend fun syncPendingData() {
        if (!isOfflineSupported()) return

        try {
            // Synchronize pending posts
            val pendingPosts = postDao!!.getPendingPosts()
            for (postEntity in pendingPosts) {
                try {
                    val post = postEntity.toDomainModel()
                    postsCollection.document(post.postId).set(post).await()
                    postDao!!.updateSyncStatus(post.postId, false)
                    Log.d("PostRepository", "Synced pending post: ${post.postId}")
                } catch (e: Exception) {
                    Log.e("PostRepository", "Failed to sync post: ${postEntity.postId}", e)
                }
            }

            // Synchronize pending comments
            val pendingComments = commentDao!!.getPendingComments()
            for (commentEntity in pendingComments) {
                try {
                    val comment = commentEntity.toDomainModel()
                    postsCollection
                        .document(commentEntity.postId)
                        .collection("comments")
                        .document(comment.commentId)
                        .set(comment)
                        .await()

                    commentDao!!.updateSyncStatus(comment.commentId, false)
                    Log.d("PostRepository", "Synced pending comment: ${comment.commentId}")

                    // Update the comment count
                    updateCommentCount(commentEntity.postId)
                } catch (e: Exception) {
                    Log.e("PostRepository", "Failed to sync comment: ${commentEntity.commentId}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error during sync", e)
        }
    }
}