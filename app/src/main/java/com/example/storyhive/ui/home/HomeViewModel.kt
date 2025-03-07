package com.example.storyhive.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Post
import com.example.storyhive.data.util.Resource
import com.example.storyhive.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = PostRepository()

    // StateFlow for managing the list of posts
    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val posts: StateFlow<Resource<List<Post>>> = _posts

    // StateFlow for managing post deletion status
    private val _deleteStatus = MutableStateFlow<Resource<Boolean>>(Resource.Loading(false))
    val deleteStatus: StateFlow<Resource<Boolean>> = _deleteStatus

    init {
        loadPosts()
    }

    /**
     * Loads the list of posts and observes changes in real-time.
     */
    fun loadPosts() {
        viewModelScope.launch {
            try {
                // Set the loading state before fetching data
                _posts.value = Resource.Loading()

                // Listen for real-time changes to posts and update UI accordingly
                repository.observePosts { posts ->
                    _posts.value = Resource.Success(posts)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading posts", e)
                _posts.value = Resource.Error("Failed to load posts: ${e.message}", emptyList())
            }
        }
    }

    /**
     * Handles liking/unliking a post by toggling the like status.
     * @param postId The ID of the post to like or unlike.
     */
    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                repository.toggleLike(postId)
            } catch (e: Exception) {
                Log.e("LikePost", "Exception: ${e.message}")
            }
        }
    }

    /**
     * Deletes a post from the database.
     * @param postId The ID of the post to delete.
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                // Set loading state before attempting deletion
                _deleteStatus.value = Resource.Loading(false)

                val result = repository.deletePost(postId)
                if (result) {
                    _deleteStatus.value = Resource.Success(true)
                    // Reload posts after a successful deletion
                    loadPosts()
                } else {
                    _deleteStatus.value = Resource.Error("Failed to delete post", false)
                }
            } catch (e: Exception) {
                Log.e("DeletePost", "Exception: ${e.message}")
                _deleteStatus.value = Resource.Error("Failed to delete post: ${e.message}", false)
            }
        }
    }

    /**
     * Refreshes the list of posts by reloading them from the database.
     */
    fun refreshPosts() {
        loadPosts()
    }
}