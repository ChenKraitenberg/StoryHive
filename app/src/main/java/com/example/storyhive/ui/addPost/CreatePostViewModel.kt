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
import com.example.storyhive.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
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

    fun createPost(context: Context, title: String, author: String, review: String, rating: Float) {
        _uiState.value = CreatePostUiState.Loading

        viewModelScope.launch {
            try {
                Log.d("CreatePostViewModel", "Starting post creation: title=$title, author=$author")

                // המרת תמונה ל-Base64 ושמירתה ב-Firestore
                val imageBase64 = selectedImageUri?.let { uri ->
                    try {
                        Log.d("CreatePostViewModel", "Encoding image to Base64...")
                        val encodedImage = storageRepository.uploadImage(context, uri, "posts_images") // משתמשים ב-context
                        Log.d("CreatePostViewModel", "Image encoded successfully!")
                        encodedImage
                    } catch (e: Exception) {
                        Log.e("CreatePostViewModel", "Failed to encode image", e)
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
                    imageBase64 = imageBase64,  // שמירת התמונה כ-Base64
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