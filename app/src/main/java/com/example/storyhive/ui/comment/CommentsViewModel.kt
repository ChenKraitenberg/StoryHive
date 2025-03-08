package com.example.storyhive.ui.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Comment
import com.example.storyhive.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

<<<<<<< HEAD
/**
 * ViewModel responsible for handling comment-related actions, including adding comments
 * and managing UI state for the comment submission process.
 */
=======
>>>>>>> main
class CommentsViewModel(private val repository: FirebaseRepository) : ViewModel() {
    private val _addCommentState = MutableStateFlow<UiState<Boolean>>(UiState.Initial)
    val addCommentState: StateFlow<UiState<Boolean>> = _addCommentState.asStateFlow()

<<<<<<< HEAD

    /**
     * Adds a comment to the given post.
     * Ensures that the comment is not empty and that the user is authenticated before proceeding.
     */
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            // Validate comment content
            if (content.trim().isEmpty()) {
                _addCommentState.value = UiState.Error("Comment cannot be empty")
                return@launch
            }

            // Check if the user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                _addCommentState.value = UiState.Error("Please sign in first")
                return@launch
            }

            // Set UI state to loading while the comment is being added
=======
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            // בדיקת תקינות תוכן התגובה
            if (content.trim().isEmpty()) {
                _addCommentState.value = UiState.Error("תגובה לא יכולה להיות ריקה")
                return@launch
            }

            // בדיקת התחברות משתמש
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                _addCommentState.value = UiState.Error("אנא התחבר")
                return@launch
            }

>>>>>>> main
            _addCommentState.value = UiState.Loading

            val comment = Comment(
                userId = currentUser.uid,
<<<<<<< HEAD
                userName = currentUser.displayName ?: "Anonymous User",
=======
                userName = currentUser.displayName ?: "משתמש אנונימי",
>>>>>>> main
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )

<<<<<<< HEAD
            // Attempt to add the comment via the repository
=======
>>>>>>> main
            repository.addComment(postId, comment) { success ->
                _addCommentState.value = if (success) {
                    UiState.Success(true)
                } else {
<<<<<<< HEAD
                    UiState.Error("Failed to add comment")
=======
                    UiState.Error("נכשל בהוספת תגובה")
>>>>>>> main
                }
            }
        }
    }
}

<<<<<<< HEAD
/**
 * Represents different UI states for handling asynchronous operations.
 */
=======
// מחלקת מצב UI
>>>>>>> main
sealed class UiState<out T> {
    object Initial : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}