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
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.example.storyhive.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * ViewModel for editing an existing post.
 * - Loads the original post details.
 * - Allows the user to update text fields and image.
 * - Handles image encoding and updates the post in Firebase.
 */
class EditPostViewModel(application: Application) : AndroidViewModel(application) {

    // Use the existing PostRepository instance from the application
    private val repository = (application as StoryHiveApplication).postRepository

    private val storageRepository = StorageRepository()

    private val _uiState = MutableLiveData<EditPostUiState>(EditPostUiState.Initial)
    val uiState: LiveData<EditPostUiState> = _uiState

    // Store the original post
    private lateinit var originalPost: Post

    // Selected image URI
    private var selectedImageUri: Uri? = null



    /**
     * Initializes the ViewModel with the original post data.
     * This method must be called before updating the post.
     */
    fun initWithPost(post: Post) {
        originalPost = post
    }


    /**
     * Stores the selected image URI for later processing.
     */
    fun setSelectedImage(uri: Uri) {
        selectedImageUri = uri
    }


    /**
     * Updates the post with new details.
     * - If a new image is selected, it is encoded into Base64.
     * - Updates the post in Firebase using the repository.
     * - Updates the UI state based on success or failure.
     */
    fun updatePost(context: Context, title: String, author: String, review: String, rating:Float = 0.0f) {
        if (!::originalPost.isInitialized) {
            _uiState.value = EditPostUiState.Error("Original post not initialized")
            return
        }

        _uiState.value = EditPostUiState.Loading

        viewModelScope.launch {
            try {
                Log.d("EditPostViewModel", "Starting post update: title=$title, author=$author")

                // Process the image if a new one was selected
                var imageBase64: String? = originalPost.imageBase64
                var imageUrl: String? = originalPost.imageUrl

                if (selectedImageUri != null) {
                    try {
                        Log.d("EditPostViewModel", "Encoding new image to Base64...")
                        imageBase64 = storageRepository.uploadImage(context, selectedImageUri!!, "posts_images")
                        Log.d("EditPostViewModel", "Image encoded successfully!")
                        // Clear the old image URL as we now have a base64 encoded image
                        imageUrl = null
                    } catch (e: Exception) {
                        Log.e("EditPostViewModel", "Failed to encode image", e)
                    }
                }

                // Create the updated post with original data + modifications
                val updatedPost = originalPost.copy(
                    bookTitle = title,
                    bookAuthor = author,
                    //review = review,
                    imageBase64 = imageBase64,
                    imageUrl = imageUrl
                )

                Log.d("EditPostViewModel", "Updated post object: $updatedPost")

                // Use the repository to update the post
                try {
                    repository.updatePost(updatedPost)
                    Log.d("EditPostViewModel", "Post updated successfully")
                    _uiState.value = EditPostUiState.Success
                } catch (e: Exception) {
                    Log.e("EditPostViewModel", "Failed to update post", e)
                    _uiState.value = EditPostUiState.Error("Failed to update post: ${e.message}")
                }

                // Update the post in Firestore
                FirebaseRepository.updatePost(updatedPost) { success ->
                    if (success) {
                        Log.d("EditPostViewModel", "Post updated successfully")
                        _uiState.value = EditPostUiState.Success
                    } else {
                        Log.e("EditPostViewModel", "Failed to update post")
                        _uiState.value = EditPostUiState.Error("Failed to update post")
                    }
                }

            } catch (e: Exception) {
                Log.e("EditPostViewModel", "Error updating post", e)
                _uiState.value = EditPostUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

}

/**
 * Represents the UI state of the post editing process.
 */
sealed class EditPostUiState {
    object Initial : EditPostUiState() // Initial state before any action
    object Loading : EditPostUiState() // Loading state while updating post
    object Success : EditPostUiState() // Success state when post is updated
    data class Error(val message: String) : EditPostUiState() // Error state with a message
 }