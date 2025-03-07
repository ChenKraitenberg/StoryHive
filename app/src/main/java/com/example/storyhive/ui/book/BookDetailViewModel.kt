// bookDetailViewModel.kt
package com.example.storyhive.ui.book

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Book
import com.example.storyhive.data.models.Review
import com.example.storyhive.data.util.Resource
import com.example.storyhive.service.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


// ViewModel for managing book details and loading related books
class BookDetailViewModel : ViewModel() {
    // StateFlow to manage the state of book details (Loading, Success, or Error)
    private val _bookDetails = MutableStateFlow<Resource<BookDetailsState>>(Resource.Loading())
    val bookDetails: StateFlow<Resource<BookDetailsState>> = _bookDetails

    // StateFlow to manage related books
    private val _relatedBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading())
    val relatedBooks: StateFlow<Resource<List<Book>>> = _relatedBooks

    /**
     * Loads book details using the provided book ID.
     * @param bookId The unique identifier for the book in the Google Books API.
     */
    fun loadBookDetails(bookId: String) {
        viewModelScope.launch {
            try {

                // Update state to loading
                _bookDetails.value = Resource.Loading()

                // Fetch book details from Google Books API
                val bookResponse = RetrofitClient.googleBooksService.getBookDetails(bookId)

                // Create a BookDetailsState object with the retrieved data
                val bookDetails = BookDetailsState(
                    id = bookId,
                    title = bookResponse.volumeInfo.title,
                    author = bookResponse.volumeInfo.authors?.firstOrNull() ?: "Unknown Author",
                    description = bookResponse.volumeInfo.description ?: "No description available",
                    coverImageUrl = bookResponse.volumeInfo.imageLinks?.thumbnail,
                    pageCount = bookResponse.volumeInfo.pageCount ?: 0,
                    publishedDate = bookResponse.volumeInfo.publishedDate ?: "Unknown"
                )

                // Update state to success with retrieved book details
                _bookDetails.value = Resource.Success(bookDetails)

                // Optionally, load related books based on the book's author
                loadRelatedBooks(bookResponse.volumeInfo.authors?.firstOrNull() ?: "")
            } catch (e: Exception) {
                // Handle error and update state accordingly
                Log.e("BookDetailViewModel", "Error loading book details", e)
                _bookDetails.value = Resource.Error("Failed to load book details: ${e.message}")
            }
        }
    }

    /**
     * Loads related books by searching for books written by the same author.
     * @param author The name of the author to search for related books.
     */
    private fun loadRelatedBooks(author: String) {
        if (author.isEmpty() || author == "Unknown Author") {
            _relatedBooks.value = Resource.Success(emptyList()) // No related books found
            return
        }
        viewModelScope.launch {
            try {
                _relatedBooks.value = Resource.Loading()

                // Fetch books by the same author from Google Books API
                val response = RetrofitClient.googleBooksService.searchBooks("inauthor:$author")
                val relatedItems = response.items?.take(5) ?: emptyList() // Limit to 5 books

                // Map API response to a list of Book objects
                val books = relatedItems.mapNotNull { googleBook ->
                    try {
                        Book(
                            id = googleBook.id,
                            title = googleBook.volumeInfo.title,
                            author = googleBook.volumeInfo.authors?.joinToString(", ") ?: author,
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
                        null // Skip books with missing data
                    }
                }

                // Update state to success with the retrieved list of related books
                _relatedBooks.value = Resource.Success(books)
            } catch (e: Exception) {
                Log.e("BookDetailViewModel", "Error loading related books", e)
                _relatedBooks.value = Resource.Error("Failed to load related books", emptyList())
            }
        }
    }
}

/**
 * Data class representing the state of book details.
 */
data class BookDetailsState(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverImageUrl: String?,
    val pageCount: Int,
    val publishedDate: String
)