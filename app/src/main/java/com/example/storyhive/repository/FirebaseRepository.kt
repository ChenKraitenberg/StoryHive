package com.example.storyhive.repository

import android.net.Uri
import android.util.Log
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.models.Comment
import com.example.storyhive.data.models.Post
import com.example.storyhive.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    // ---------------------------
    // Authentication
    // ---------------------------
    fun getCurrentUser() = auth.currentUser

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }


    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

    // ---------------------------
    // User Data
    // ---------------------------
    fun saveUserData(user: User, onComplete: (Boolean) -> Unit) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)

        userRef.set(user)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Error saving user data", e)
                onComplete(false)
            }
    }


    fun getUserData(uid: String, onResult: (User?) -> Unit) {
        val userDocRef = firestore.collection("users").document(uid)
        userDocRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    onResult(user)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // ---------------------------
    // Post Data
    // ---------------------------
    fun createPost(post: Post, onResult: (Boolean) -> Unit) {
        try {
            Log.d("FirebaseRepository", "Starting to create post: ${post.bookTitle}")
            Log.d("FirebaseRepository", "Post details: userId=${post.userId}, authorName=${post.userDisplayName}")

            val postDocRef = firestore.collection("posts").document()
            val postId = postDocRef.id
            Log.d("FirebaseRepository", "Generated postId: $postId")

            val postWithId = post.copy(postId = postId)

            postDocRef.set(postWithId)
                .addOnSuccessListener {
                    Log.d("FirebaseRepository", "Post created successfully")
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Failed to create post: ${e.message}", e)
                    onResult(false)
                }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Exception creating post: ${e.message}", e)
            onResult(false)
        }
    }

    fun getAllPosts(onResult: (List<Post>) -> Unit) {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = querySnapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                onResult(posts)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun getPostsByUser(uid: String, onResult: (List<Post>) -> Unit) {
        firestore.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = querySnapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                onResult(posts)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun updatePost(post: Post, onResult: (Boolean) -> Unit) {
        val postDocRef = firestore.collection("posts").document(post.postId)
        postDocRef.set(post)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    fun deletePost(postId: String, onResult: (Boolean) -> Unit) {
        val postDocRef = firestore.collection("posts").document(postId)
        postDocRef.delete()
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    // ---------------------------
    // User Profile Image Caching
    // ---------------------------
    fun syncUserProfileImages(imageCacheManager: ImageCacheManager) {
        // Get all users from the server
        val usersCollection = firestore.collection("users")
        usersCollection.get()
            .addOnSuccessListener { usersSnapshot ->
                // For each user, check if they have a profile image
                for (userDoc in usersSnapshot.documents) {
                    val photoUrl = userDoc.getString("photoUrl")

                    // If there's a profile image URL, ensure it's cached
                    if (!photoUrl.isNullOrEmpty()) {
                        // Using a coroutine scope to call the suspend function
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                imageCacheManager.cacheImage(photoUrl)
                                Log.d("FirebaseRepository", "Cached profile image: $photoUrl")
                            } catch (e: Exception) {
                                Log.e("FirebaseRepository", "Error caching image: $photoUrl", e)
                            }
                        }
                    }
                }
                Log.d("FirebaseRepository", "Starting profile images sync")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Error syncing profile images", e)
            }
    }

    // ---------------------------
    // Upload image to Firebase Storage
    // ---------------------------
    fun uploadImageToStorage(uri: Uri, folder: String, onResult: (Boolean, String?) -> Unit) {
        val fileName = "${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("$folder/$fileName")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    onResult(true, downloadUri.toString())
                }
            }
            .addOnFailureListener {
                onResult(false, null)
            }
    }


    // ---------------------------
    // Comments
    // ---------------------------
    fun addComment(postId: String, comment: Comment, onResult: (Boolean) -> Unit) {
        try {
            val commentRef = firestore.collection("posts")
                .document(postId)
                .collection("comments")
                .document()

            val commentWithId = comment.copy(commentId = commentRef.id)

            firestore.runTransaction { transaction ->
                // הוספת התגובה
                transaction.set(commentRef, commentWithId)

                // עדכון ספירת תגובות בפוסט
                val postRef = firestore.collection("posts").document(postId)
                val snapshot = transaction.get(postRef)
                val currentCount = snapshot.getLong("commentCount") ?: 0
                transaction.update(postRef, "commentCount", currentCount + 1)
            }.addOnSuccessListener {
                Log.d("FirebaseRepository", "Comment added successfully to post $postId")
                onResult(true)
            }.addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Failed to add comment to post $postId", e)
                onResult(false)
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Exception adding comment", e)
            onResult(false)
        }
    }

    private fun updateCommentCount(postId: String, onResult: (Boolean) -> Unit) {
        val postRef = firestore.collection("posts").document(postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentCount = snapshot.getLong("commentCount") ?: 0
            transaction.update(postRef, "commentCount", currentCount + 1)
        }.addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }
}
