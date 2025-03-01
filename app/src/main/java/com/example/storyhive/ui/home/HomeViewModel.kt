package com.example.storyhive.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Post
import com.example.storyhive.repository.FirebaseRepository
import com.example.storyhive.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = PostRepository()
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    init {
        loadPosts()
    }

    private fun loadPosts() {
        _isLoading.value = true
        repository.observePosts { posts ->
            _posts.postValue(posts)
            _isLoading.postValue(false)
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
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
            } catch (e: Exception) {
                Log.e("LikePost", "Exception: ${e.message}")
            }
        }
    }

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
            }
        }
    }

    fun refreshPosts()
    {
        loadPosts()

    }


}

private fun Unit.onFailure(action: (Throwable) -> Unit) {
    action(Throwable("Failed"))

}
