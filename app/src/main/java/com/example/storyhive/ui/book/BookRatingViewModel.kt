package com.example.storyhive.ui.book

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storyhive.data.models.Review
import com.google.firebase.firestore.FirebaseFirestore

class BookRatingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _bookRatings = MutableLiveData<RatingData>()
    val bookRatings: LiveData<RatingData> = _bookRatings

    fun fetchBookRatings(bookId: String) {
        firestore.collection("reviews")
            .whereEqualTo("bookId", bookId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // טיפול בשגיאה
                    return@addSnapshotListener
                }

                val reviews = snapshot?.toObjects(Review::class.java) ?: emptyList()
                val averageRating = if (reviews.isNotEmpty()) {
                    reviews.map { it.rating }.average().toFloat()
                } else 0f

                _bookRatings.value = RatingData(
                    averageRating = averageRating,
                    totalRatings = reviews.size,
                    reviews = reviews
                )
            }
    }

    fun addReview(review: Review) {
        firestore.collection("reviews")
            .add(review)
            .addOnSuccessListener {
                // הצלחת הוספת ביקורת
            }
            .addOnFailureListener {
                // טיפול בשגיאה
            }
    }
}

data class RatingData(
    val averageRating: Float,
    val totalRatings: Int,
    val reviews: List<Review>
)