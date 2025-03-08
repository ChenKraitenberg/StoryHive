package com.example.storyhive.ui.home

import android.util.Log
<<<<<<< HEAD
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Post
import com.example.storyhive.data.util.Resource
import com.example.storyhive.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
=======
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Post
import com.example.storyhive.repository.FirebaseRepository
import com.example.storyhive.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
>>>>>>> main
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = PostRepository()
<<<<<<< HEAD

    // StateFlow for managing the list of posts
    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val posts: StateFlow<Resource<List<Post>>> = _posts

    // StateFlow for managing post deletion status
    private val _deleteStatus = MutableStateFlow<Resource<Boolean>>(Resource.Loading(false))
    val deleteStatus: StateFlow<Resource<Boolean>> = _deleteStatus
=======
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus
>>>>>>> main

    init {
        loadPosts()
    }

<<<<<<< HEAD
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
=======
    private fun loadPosts() {
        _isLoading.value = true
        repository.observePosts { posts ->
            _posts.postValue(posts)
            _isLoading.postValue(false)
        }
    }

>>>>>>> main
    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
<<<<<<< HEAD
                repository.toggleLike(postId)
=======
                val postRef = FirebaseFirestore.getInstance().collection("posts").document(postId)

                FirebaseFirestore.getInstance().runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

                    if (likedBy.contains(userId)) {
                        likedBy.remove(userId)  // הסרה במקרה של Unlike
                    } else {
                        likedBy.add(userId)  // הוספת הלייק
                    }

                    transaction.update(postRef, "likedBy", likedBy)
                    transaction.update(postRef, "likes", likedBy.size)
                }.addOnSuccessListener {
                    Log.d("Firestore", "Successfully updated likes for post $postId")
                }.addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating likes: ${e.message}")
                }
>>>>>>> main
            } catch (e: Exception) {
                Log.e("LikePost", "Exception: ${e.message}")
            }
        }
    }

<<<<<<< HEAD
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
=======
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val result = repository.deletePost(postId) // ✅ ודא שהפונקציה מחזירה ערך
                _deleteStatus.postValue(result)
                if (result) {
                    loadPosts() // ✅ טען מחדש את הפוסטים לאחר מחיקה מוצלחת
                }
            } catch (e: Exception) {
                Log.e("DeletePost", "Exception: ${e.message}")
                _deleteStatus.postValue(false) // מחזיר שגיאה אם יש בעיה
>>>>>>> main
            }
        }
    }

<<<<<<< HEAD
    /**
     * Refreshes the list of posts by reloading them from the database.
     */
    fun refreshPosts() {
        loadPosts()
    }
}
=======
    fun refreshPosts()
    {
        loadPosts()

    }


}

private fun Unit.onFailure(action: (Throwable) -> Unit) {
    action(Throwable("Failed"))

}
>>>>>>> main
