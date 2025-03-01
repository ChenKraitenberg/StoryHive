package com.example.storyhive.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.repository.PostRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = PostRepository()

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    fun deletePost(postId: String) {
        viewModelScope.launch {  // ✅ קריאה למחיקה ברקע
            val result = repository.deletePost(postId)
            _deleteStatus.postValue(result)
        }
    }
}
