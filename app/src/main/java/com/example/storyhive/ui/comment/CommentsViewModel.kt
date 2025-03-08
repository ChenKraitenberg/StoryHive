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


/**
 * ViewModel responsible for handling comment-related actions, including adding comments
 * and managing UI state for the comment submission process.
 */
class CommentsViewModel(private val repository: FirebaseRepository) : ViewModel() {
    private val _addCommentState = MutableStateFlow<UiState<Boolean>>(UiState.Initial)
    val addCommentState: StateFlow<UiState<Boolean>> = _addCommentState.asStateFlow()

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
            _addCommentState.value = UiState.Loading

            val comment = Comment(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonymous User",
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )

            // Attempt to add the comment via the repository
            repository.addComment(postId, comment) { success ->
                _addCommentState.value = if (success) {
                    UiState.Success(true)
                } else {

                    UiState.Error("Failed to add comment")
                }
            }
        }
    }
}

/**
 * Represents different UI states for handling asynchronous operations.
 */
sealed class UiState<out T> {
    object Initial : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}