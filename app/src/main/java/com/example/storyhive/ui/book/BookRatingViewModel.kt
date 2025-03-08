package com.example.storyhive.ui.book

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storyhive.data.models.Review
import com.google.firebase.firestore.FirebaseFirestore


/**
 * ViewModel responsible for handling book ratings and reviews.
 */
class BookRatingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // LiveData to hold rating data for a specific book
    private val _bookRatings = MutableLiveData<RatingData>()
    val bookRatings: LiveData<RatingData> = _bookRatings

    /**
     * Fetches book ratings and reviews from Firestore based on the given book ID.
     * @param bookId The ID of the book to retrieve ratings for.
     */
    fun fetchBookRatings(bookId: String) {
        firestore.collection("reviews")
            .whereEqualTo("bookId", bookId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Handle error (optional: log error message)
                    return@addSnapshotListener
                }

                val reviews = snapshot?.toObjects(Review::class.java) ?: emptyList()
                val averageRating = if (reviews.isNotEmpty()) {
                    reviews.map { it.rating }.average().toFloat()
                } else 0f
                // Update LiveData with the new rating data
                _bookRatings.value = RatingData(
                    averageRating = averageRating,
                    totalRatings = reviews.size,
                    reviews = reviews
                )
            }
    }


    /**
     * Adds a new user review to Firestore.
     * @param review The Review object to be added.
     */
    fun addReview(review: Review) {
        firestore.collection("reviews")
            .add(review)
            .addOnSuccessListener {
                // Successfully added review (optional: trigger UI update)
            }
            .addOnFailureListener {
                // Handle error (optional: log error message)
            }
            .addOnFailureListener {
            }
    }
}



/**
 * Data class representing the rating and review details of a book.
 * @property averageRating The average rating of the book.
 * @property totalRatings The total number of reviews.
 * @property reviews The list of reviews for the book.
 */
data class RatingData(
    val averageRating: Float,
    val totalRatings: Int,
    val reviews: List<Review>
)