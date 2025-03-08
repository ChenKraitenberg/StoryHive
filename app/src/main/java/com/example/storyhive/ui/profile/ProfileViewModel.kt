package com.example.storyhive.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.repository.PostRepository
import kotlinx.coroutines.launch


/**
 * ViewModel for managing profile-related operations, such as deleting user posts.
 */
class ProfileViewModel : ViewModel() {
    private val repository = PostRepository()

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    /**
     * Deletes a post by its ID and updates the delete status.
     * This runs in the background using a coroutine.
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = repository.deletePost(postId)
            _deleteStatus.postValue(result)
        }
    }
}

