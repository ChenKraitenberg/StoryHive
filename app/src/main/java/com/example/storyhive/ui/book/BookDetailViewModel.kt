// bookDetailViewModel.kt
package com.example.storyhive.ui.book

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyhive.data.models.Book
import com.example.storyhive.data.models.Review
import com.example.storyhive.service.RetrofitClient
import kotlinx.coroutines.launch

class BookDetailViewModel : ViewModel() {
    private val _bookDetails = MutableLiveData<BookDetailsState?>()
    val bookDetails: MutableLiveData<BookDetailsState?> = _bookDetails

    fun loadBookDetails(bookId: String) {
        viewModelScope.launch {
            try {
                val bookResponse = RetrofitClient.googleBooksService.getBookDetails(bookId)

                val bookDetails = BookDetailsState(
                    id = bookId,
                    title = bookResponse.volumeInfo.title,
                    author = bookResponse.volumeInfo.authors?.firstOrNull() ?: "Unknown Author",
                    description = bookResponse.volumeInfo.description ?: "No description available",
                    coverImageUrl = bookResponse.volumeInfo.imageLinks?.thumbnail,
                    pageCount = bookResponse.volumeInfo.pageCount ?: 0,
                    publishedDate = bookResponse.volumeInfo.publishedDate ?: "Unknown"
                )

                _bookDetails.value = bookDetails
            } catch (e: Exception) {
                _bookDetails.value = null
            }
        }
    }
}

data class BookDetailsState(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverImageUrl: String?,
    val pageCount: Int,
    val publishedDate: String
)