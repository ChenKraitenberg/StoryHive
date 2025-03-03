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

class CommentsViewModel(private val repository: FirebaseRepository) : ViewModel() {
    private val _addCommentState = MutableStateFlow<UiState<Boolean>>(UiState.Initial)
    val addCommentState: StateFlow<UiState<Boolean>> = _addCommentState.asStateFlow()

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

            _addCommentState.value = UiState.Loading

            val comment = Comment(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "משתמש אנונימי",
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )

            repository.addComment(postId, comment) { success ->
                _addCommentState.value = if (success) {
                    UiState.Success(true)
                } else {
                    UiState.Error("נכשל בהוספת תגובה")
                }
            }
        }
    }
}

// מחלקת מצב UI
sealed class UiState<out T> {
    object Initial : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}