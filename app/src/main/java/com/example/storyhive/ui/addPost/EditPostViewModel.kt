package com.example.storyhive.ui.addPost

import StorageRepository
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Post
import com.example.storyhive.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.net.URLEncoder

class EditPostViewModel : ViewModel() {
    private val storageRepository = StorageRepository()

    private val _uiState = MutableLiveData<EditPostUiState>(EditPostUiState.Initial)
    val uiState: LiveData<EditPostUiState> = _uiState

    // Store the original post
    private lateinit var originalPost: Post

    // Selected image URI
    private var selectedImageUri: Uri? = null

    fun initWithPost(post: Post) {
        originalPost = post
    }

    fun setSelectedImage(uri: Uri) {
        selectedImageUri = uri
    }

    fun updatePost(context: Context, title: String, author: String, review: String, rating: Float) {
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
                    review = review,
                    rating = rating,
                    imageBase64 = imageBase64,
                    imageUrl = imageUrl
                )

                Log.d("EditPostViewModel", "Updated post object: $updatedPost")

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

sealed class EditPostUiState {
    object Initial : EditPostUiState()
    object Loading : EditPostUiState()
    object Success : EditPostUiState()
    data class Error(val message: String) : EditPostUiState()
}