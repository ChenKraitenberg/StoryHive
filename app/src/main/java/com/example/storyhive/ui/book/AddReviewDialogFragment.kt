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

class AddReviewDialogFragment : DialogFragment() {
    private lateinit var bookId: String
    private val viewModel: BookRatingViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

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

                viewModel.addReview(review)
            }
            .create()
    }
}