// File: SearchViewModel.kt
package com.example.storyhive.ui.search

import android.util.Log
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
                val items = response.items ?: emptyList()  // טיפול במקרה של null

                val books = items.mapNotNull { googleBook ->
                    try {
                        // המרה של Google Book ל-Book מקומי
                        Book(
                            id = googleBook.id,
                            title = googleBook.volumeInfo.title,
                            author = googleBook.volumeInfo.authors?.joinToString(", ")
                                ?: "Unknown Author",
                            description = googleBook.volumeInfo.description ?: "",
                            coverUrl = googleBook.volumeInfo.imageLinks?.thumbnail?.replace(
                                "http:",
                                "https:"
                            ) ?: "",
                            genre = "Unknown",
                            rating = 0f,
                            pageCount = googleBook.volumeInfo.pageCount ?: 0,
                            publishedDate = googleBook.volumeInfo.publishedDate ?: ""
                        )
                    } catch (e: Exception) {
                        null  // דלג על ספרים עם נתונים חסרים
                    }
                }
                _searchResults.value = books
            } catch (e: Exception) {
                // טיפול בשגיאה
                _searchResults.value = emptyList()
                // כדאי להוסיף לוג לשגיאה
                Log.e("SearchViewModel", "Error searching books", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}