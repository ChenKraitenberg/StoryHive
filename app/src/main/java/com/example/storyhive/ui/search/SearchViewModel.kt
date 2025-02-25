package com.example.storyhive.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storyhive.data.models.Book
import androidx.lifecycle.viewModelScope
import com.example.storyhive.service.RetrofitClient
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val _searchResults = MutableLiveData<List<Book>>()
    val searchResults: LiveData<List<Book>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun searchBooks(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.googleBooksService.searchBooks(query)
                val books = response.items.mapNotNull { googleBook ->
                    googleBook.volumeInfo.imageLinks?.thumbnail?.let {
                        Book(
                            id = googleBook.id,
                            title = googleBook.volumeInfo.title,
                            author = googleBook.volumeInfo.authors?.firstOrNull() ?: "Unknown Author",
                            genre = "Unknown",
                            rating = 0f,
                            coverUrl = it
                        )
                    }
                }
                _searchResults.value = books
            } catch (e: Exception) {
                // Handle error
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}