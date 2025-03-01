package com.example.storyhive.ui.book

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storyhive.data.models.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class BookReviewViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _bookReviews = MutableLiveData<List<Review>>()
    val bookReviews: LiveData<List<Review>> = _bookReviews

    fun loadReviewsForBook(bookId: String) {
        firestore.collection("books").document(bookId)
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // handle error
                    return@addSnapshotListener
                }

                val reviews = snapshot?.toObjects(Review::class.java) ?: emptyList()
                _bookReviews.value = reviews
            }
    }
}