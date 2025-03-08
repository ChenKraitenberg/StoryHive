package com.example.storyhive.ui.comment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.storyhive.R
import com.example.storyhive.data.models.Comment
import com.example.storyhive.databinding.ItemCommentBinding
import com.google.firebase.auth.FirebaseAuth

<<<<<<< HEAD

/**
 * Adapter for displaying a list of comments in a RecyclerView.
 * Handles UI updates, formatting timestamps, and managing comment deletion.
 */
=======
>>>>>>> main
class CommentsAdapter(
    private var comments: List<Comment> = emptyList(),
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

<<<<<<< HEAD
    // Updates the comment list and refreshes the UI
=======
>>>>>>> main
    fun updateComments(newComments: List<Comment>) {
        this.comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

<<<<<<< HEAD
        // Binds the comment data to the UI elements
=======
>>>>>>> main
        fun bind(comment: Comment) {
            binding.apply {
                // Set comment text and username
                commentText.text = comment.content
                userNameText.text = comment.userName

<<<<<<< HEAD
                // Format and display timestamp
=======
                // Format timestamp
>>>>>>> main
                val formattedTime = android.text.format.DateFormat.format(
                    "MMM dd, yyyy HH:mm",
                    comment.timestamp
                )
                commentTimestamp.text = formattedTime.toString()

<<<<<<< HEAD
                // Show delete button only for the comment's author
=======
                // Set visibility of delete button based on user
>>>>>>> main
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                deleteButton.visibility = if (currentUserId == comment.userId) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

<<<<<<< HEAD
                // Handle delete button click event
=======
                // Set click listener for delete button
>>>>>>> main
                deleteButton.setOnClickListener {
                    onDeleteClick(comment)
                }
            }
        }
    }
}