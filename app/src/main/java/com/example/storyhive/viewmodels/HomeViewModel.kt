package com.example.storyhive.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storyhive.data.models.Post

class HomeViewModel : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    fun setMockPosts() {
        val mockPosts = listOf(
            Post("1", "123", "Chen Ktaitenberg", "Romeo and Juliet", "William Shakespeare", "A classic romantic tragedy", null, 100, System.currentTimeMillis(), listOf()),
            Post("2", "124", "Avital Aizenkot", "Harry Potter", "J.K. Rowling", "An amazing fantasy series", null, 250, System.currentTimeMillis(), listOf())
        )
        _posts.value = mockPosts
    }
}
