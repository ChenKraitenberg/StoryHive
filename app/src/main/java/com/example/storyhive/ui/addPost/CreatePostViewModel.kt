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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URLEncoder
import kotlin.coroutines.resume

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
                        val encodedImage = storageRepository.uploadImage(context, uri, "posts_images")
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

                val userId = currentUser.uid
                val userName = currentUser.displayName ?: "Anonymous"

                // בדיקת תמונת פרופיל - השינוי העיקרי כאן
                var userProfileImage = currentUser.photoUrl?.toString() ?: ""

// אם אין תמונת פרופיל ב-FirebaseAuth, נסה להביא מ-Firestore
                if (userProfileImage.isEmpty()) {
                    try {
                        userProfileImage = getUserProfileImageFromFirestore(userId)
                        Log.d("CreatePostViewModel", "Got profile image from Firestore: $userProfileImage")
                    } catch (e: Exception) {
                        Log.e("CreatePostViewModel", "Failed to get profile image from Firestore", e)
                    }

                    // אם עדיין אין תמונת פרופיל, השתמש בברירת מחדל
                    if (userProfileImage.isEmpty()) {
                        // אפשרות 1: להשתמש במשאב מקומי
                       // userProfileImage = "android.resource://com.example.storyhive/drawable/default_avatar"

                        // אפשרות 2: להשתמש בשירות חיצוני
                         userProfileImage = "https://ui-avatars.com/api/?name=" +
                            URLEncoder.encode(userName, "UTF-8") + "&background=random&size=100"

                        Log.d("CreatePostViewModel", "Using default profile image")
                    }
                }
                val post = Post(
                    postId = "",
                    userId = userId,
                    userDisplayName = userName,
                    userProfileImage = userProfileImage,
                    bookTitle = title,
                    bookAuthor = author,
                    review = review,
                    likes = 0,
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

//    fun getUserProfileImageFromFirestore(userId: String): String {
//        val firestore = FirebaseFirestore.getInstance()
//        var profileImage = ""
//
//        firestore.collection("users").document(userId)
//            .get()
//            .addOnSuccessListener { document ->
//                if (document != null && document.contains("profileImage")) {
//                    profileImage = document.getString("profileImage") ?: ""
//                }
//            }
//            .addOnFailureListener {
//                Log.e("Firestore", "Failed to fetch user profile image")
//            }
//
//        return profileImage
//    }
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