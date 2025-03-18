package com.example.storyhive.ui.addPost

import StorageRepository
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.storyhive.StoryHiveApplication
import com.example.storyhive.data.models.Post
import com.example.storyhive.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.storyhive.repository.PostRepository

/**
 * ViewModel for creating a new post.
 * Manages user input, image processing, and data storage in Firebase.
 */
class CreatePostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StoryHiveApplication).postRepository
    private val storageRepository = StorageRepository()

    private val _uiState = MutableLiveData<CreatePostUiState>(CreatePostUiState.Initial)
    val uiState: LiveData<CreatePostUiState> = _uiState

    //Stores the selected image URI
    private var selectedImageUri: Uri? = null

    fun setSelectedImage(uri: Uri) {
        selectedImageUri = uri
    }

    /**
     * Creates a new post.
     * - Processes and encodes the selected image (if available).
     * - Retrieves user data from Firebase Authentication.
     * - Stores the post in Firebase Firestore.
     * - Updates UI state based on success or failure.
     */
    fun createPost(context: Context, title: String, author: String, review: String, rating: Float) {
        _uiState.value = CreatePostUiState.Loading

        viewModelScope.launch {
            try {
                Log.d("CreatePostViewModel", "Starting post creation: title=$title, author=$author")

                // Handle image processing
                var imageBase64: String? = null

                // Process image only if one is selected
                selectedImageUri?.let { uri ->
                    try {
                        Log.d("CreatePostViewModel", "Processing image...")
                        // Upload image and get base64 string
                        imageBase64 = storageRepository.uploadImage(context, uri, "posts_images")
                        Log.d("CreatePostViewModel", "Image processed successfully: ${imageBase64?.take(50)}...")
                    } catch (e: Exception) {
                        Log.e("CreatePostViewModel", "Failed to process image", e)
                        // Continue without image if there's an error
                    }
                }

                // Get current user info
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.e("CreatePostViewModel", "User not authenticated")
                    _uiState.value = CreatePostUiState.Error("User not authenticated")
                    return@launch
                }

                val userId = currentUser.uid
                val userName = currentUser.displayName ?: "Anonymous"
                var userProfileImage = currentUser.photoUrl?.toString() ?: ""

                // Create post object with image data
                val post = Post(
                    postId = "",
                    userId = userId,
                    userDisplayName = userName,
                    userProfileImage = userProfileImage,
                    bookTitle = title,
                    bookAuthor = author,
                    //review = review,
                    imageBase64 = imageBase64,  // Set the image data directly
                    rating = 0.0f,
                    likes = 0,
                    timestamp = System.currentTimeMillis()
                )

                Log.d("CreatePostViewModel", "Created post object: $post")
                Log.d("CreatePostViewModel", "Post has image: ${imageBase64 != null}")


                try {
                    repository.createPost(post)
                    Log.d("CreatePostViewModel", "Post created successfully")
                    _uiState.value = CreatePostUiState.Success
                } catch (e: Exception) {
                    Log.e("CreatePostViewModel", "Failed to create post", e)
                    _uiState.value = CreatePostUiState.Error("Failed to save post: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("CreatePostViewModel", "Error creating post", e)
                _uiState.value = CreatePostUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Retrieves the user's profile image URL from Firestore.
     * @param userId The ID of the user whose profile image is being fetched.
     * @return The profile image URL or an empty string if not found.
     */
    suspend fun getUserProfileImageFromFirestore(userId: String): String {
        return suspendCancellableCoroutine { continuation ->
            val firestore = FirebaseFirestore.getInstance()

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val profileImage = if (document != null && document.contains("profileImage")) {
                        document.getString("profileImage") ?: ""
                    } else {
                        ""
                    }
                    Log.d("CreatePostViewModel", "Got profile image from Firestore: $profileImage")
                    continuation.resume(profileImage)
                }
                .addOnFailureListener { e ->
                    Log.e("CreatePostViewModel", "Failed to fetch user profile image", e)
                    continuation.resume("")
                }
        }
    }

    /**
     * Validates the input fields for post creation.
     * Ensures the title, author, and review fields are not empty.
     * @return True if all fields are valid, false otherwise.
     */
    private fun validateInput(title: String, author: String, review: String): Boolean {
        return title.isNotBlank() && author.isNotBlank() && review.isNotBlank()
    }
}

//Represents the UI state of the post creation process
sealed class CreatePostUiState {
    object Initial : CreatePostUiState()
    object Loading : CreatePostUiState()
    object Success : CreatePostUiState()
    data class Error(val message: String) : CreatePostUiState()
}