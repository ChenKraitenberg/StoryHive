package com.example.storyhive.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.repository.PostRepository
import kotlinx.coroutines.launch

<<<<<<< HEAD
/**
 * ViewModel for managing profile-related operations, such as deleting user posts.
 */
=======
>>>>>>> main
class ProfileViewModel : ViewModel() {
    private val repository = PostRepository()

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

<<<<<<< HEAD
    /**
     * Deletes a post by its ID and updates the delete status.
     * This runs in the background using a coroutine.
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
=======
    fun deletePost(postId: String) {
        viewModelScope.launch {  // ✅ קריאה למחיקה ברקע
>>>>>>> main
            val result = repository.deletePost(postId)
            _deleteStatus.postValue(result)
        }
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> main
