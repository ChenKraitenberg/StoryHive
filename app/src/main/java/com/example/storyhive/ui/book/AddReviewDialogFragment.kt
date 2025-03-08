package com.example.storyhive.ui.book

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.RatingBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.storyhive.R
import com.example.storyhive.data.models.Review
import com.google.firebase.auth.FirebaseAuth

<<<<<<< HEAD

/**
 * **AddReviewDialogFragment** - A dialog fragment for adding a book review.
 * - Allows users to enter a rating and review text.
 * - Submits the review to the ViewModel upon confirmation.
 */
=======
>>>>>>> main
class AddReviewDialogFragment : DialogFragment() {
    private lateinit var bookId: String
    private val viewModel: BookRatingViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

<<<<<<< HEAD

    /**
     * Creates and returns the review dialog.
     * - Inflates the layout for the review input.
     * - Handles the submission of the review.
     */
=======
>>>>>>> main
    @SuppressLint("MissingInflatedId")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.reviewRatingBar)
        val reviewText = dialogView.findViewById<EditText>(R.id.reviewEditText)

        return AlertDialog.Builder(requireContext())
            .setTitle("Add Review")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val review = Review(
                    userId = auth.currentUser?.uid ?: return@setPositiveButton,
                    bookId = bookId,
                    userName = auth.currentUser?.displayName ?: "Anonymous",
                    content = reviewText.text.toString(),
                    rating = ratingBar.rating
                )
<<<<<<< HEAD
                // Pass the review to the ViewModel for storage
=======

>>>>>>> main
                viewModel.addReview(review)
            }
            .create()
    }
}