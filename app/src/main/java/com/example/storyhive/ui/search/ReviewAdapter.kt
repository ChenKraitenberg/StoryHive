package com.example.storyhive.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.storyhive.R
import com.example.storyhive.data.models.Review

class ReviewAdapter(
    private var reviews: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount() = reviews.size

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val reviewTextView: TextView = itemView.findViewById(R.id.reviewTextView)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)

        fun bind(review: Review) {
            userNameTextView.text = review.userName
            reviewTextView.text = review.content
            ratingBar.rating = review.rating
        }
    }
}