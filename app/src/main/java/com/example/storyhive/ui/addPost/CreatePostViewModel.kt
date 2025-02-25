package com.example.storyhive.ui.addPost

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Post
import com.example.storyhive.repository.FirebaseRepository
import com.example.storyhive.repository.PostRepository
import com.example.storyhive.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {
    private val repository = PostRepository()
    private val storageRepository = StorageRepository()

    private val _uiState = MutableLiveData<CreatePostUiState>(CreatePostUiState.Initial)
    val uiState: LiveData<CreatePostUiState> = _uiState

    // מחזיק את ה-URI של התמונה שנבחרה
    private var selectedImageUri: Uri? = null

    fun setSelectedImage(uri: Uri) {
        selectedImageUri = uri
    }

//    fun createPost(title: String, description: String, review: String, rating: Float) {
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        if (currentUser != null) {
//            val userId = currentUser.uid
//            val newPostId = FirebaseFirestore.getInstance().collection("posts").document().id
//
//            // שליפת שם המשתמש מה-Firestore
//            FirebaseFirestore.getInstance().collection("users").document(userId)
//                .get()
//                .addOnSuccessListener { document ->
//                    val userName = document.getString("displayName") ?: "Unknown"
//                    val userProfileImage = document.getString("profileImageUrl") ?: ""
//
//                    val post = Post(
//                        postId = newPostId,
//                        userId = userId,
//                        userDisplayName = userName, // משתמשים בשם השמור בפיירסטור
//                        userProfileImage = userProfileImage, // תמונת הפרופיל של המשתמש
//                        bookTitle = title,
//                        bookDescription = description,
//                        likes = 0
//                    )
//
//                    FirebaseFirestore.getInstance().collection("posts").document(newPostId)
//                        .set(post)
//                        .addOnSuccessListener {
//                            Log.d("Firestore", "Post added successfully")
//                        }
//                        .addOnFailureListener { e ->
//                            Log.e("Firestore", "Error adding post", e)
//                        }
//                }
//                .addOnFailureListener { e ->
//                    Log.e("Firestore", "Error retrieving user data", e)
//                }
//        } else {
//            Log.e("Firestore", "User is not logged in")
//        }
//        FirebaseFirestore.getInstance().collection("posts")
//            .get()
//            .addOnSuccessListener { documents ->
//                for (document in documents) {
//                    val post = document.toObject(Post::class.java)
//                    Log.d("Firestore", "Post: ${post.bookTitle}, Review: ${post.bookDescription}")
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Firestore", "Error fetching posts", exception)
//            }
//
//    }

    fun createPost(title: String, author: String, review: String, rating: Float) {
        _uiState.value = CreatePostUiState.Loading

        viewModelScope.launch {
            try {
                Log.d("CreatePostViewModel", "Starting post creation: title=$title, author=$author")

                // העלאת תמונה אם נבחרה
                val imageUrl = selectedImageUri?.let { uri ->
                    try {
                        Log.d("CreatePostViewModel", "Uploading image...")
                        val url = StorageRepository().uploadImage(uri, "book_images")
                        Log.d("CreatePostViewModel", "Image uploaded: $url")
                        url
                    } catch (e: Exception) {
                        Log.e("CreatePostViewModel", "Failed to upload image", e)
                        null
                    }
                }

                // קבל את פרטי המשתמש הנוכחי
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.e("CreatePostViewModel", "User not authenticated")
                    _uiState.value = CreatePostUiState.Error("User not authenticated")
                    return@launch
                }

                // בנה את אובייקט הפוסט
                val post = Post(
                    userId = currentUser.uid,
                    userDisplayName = currentUser.displayName ?: "Unknown",
                    userProfileImage = currentUser.photoUrl?.toString() ?: "",
                    bookTitle = title,
                    bookAuthor = author,
                    imageUrl = imageUrl,
                    review = review,
                    rating = rating,
                    timestamp = System.currentTimeMillis()
                )

                Log.d("CreatePostViewModel", "Created post object: $post")

                // שמור את הפוסט ב-Firestore
                FirebaseRepository.createPost(post) { success ->
                    if (success) {
                        Log.d("CreatePostViewModel", "Post created successfully")
                        _uiState.value = CreatePostUiState.Success
                    } else {
                        Log.e("CreatePostViewModel", "Failed to create post")
                        _uiState.value = CreatePostUiState.Error("Failed to save post")
                    }
                }
            } catch (e: Exception) {
                Log.e("CreatePostViewModel", "Error creating post", e)
                _uiState.value = CreatePostUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    private fun validateInput(title: String, author: String, review: String): Boolean {
        return title.isNotBlank() && author.isNotBlank() && review.isNotBlank()
    }
}

private fun Any.onFailure(action: (Throwable) -> Unit): (Throwable) -> Unit {
    return action
}

private fun Unit.onSuccess(action: Function1<*, *>): Any {
    return action


}


sealed class CreatePostUiState {
    object Initial : CreatePostUiState()
    object Loading : CreatePostUiState()
    object Success : CreatePostUiState()
    data class Error(val message: String) : CreatePostUiState()
}