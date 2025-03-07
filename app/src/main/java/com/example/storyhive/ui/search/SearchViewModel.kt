package com.example.storyhive.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Book
import com.example.storyhive.data.util.Resource
import com.example.storyhive.service.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class SearchViewModel : ViewModel() {
    // StateFlow to manage search results using Resource class (Loading, Success, or Error)
    private val _searchResults = MutableStateFlow<Resource<List<Book>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<Book>>> = _searchResults


    /**
     * Performs a search query using Google Books API.
     * Updates the StateFlow with the search results.
     * If the query is empty, clears results.
     */
    fun searchBooks(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            // Set state to Loading while fetching data
            _searchResults.value = Resource.Loading()

            try {
                // Make a request to Google Books API
                val response = RetrofitClient.googleBooksService.searchBooks(query)
                val items = response.items ?: emptyList()

                // Convert Google Book API response to local Book model
                val books = items.mapNotNull { googleBook ->
                    try {
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
                        null  // Skip books with missing or invalid data
                    }
                }

                // Update state with successful search results
                _searchResults.value = Resource.Success(books)
            } catch (e: Exception) {
                // Log the error and update state with failure message
                Log.e("SearchViewModel", "Error searching books", e)
                _searchResults.value = Resource.Error(
                    message = "Failed to search books: ${e.message}",
                    data = emptyList() // Optionally return previous results or an empty list
                )
            }
        }
    }

    /**
     * Clears the search results, resetting the state to an empty list.
     */
    fun clearSearchResults() {
        _searchResults.value = Resource.Success(emptyList())
    }
}