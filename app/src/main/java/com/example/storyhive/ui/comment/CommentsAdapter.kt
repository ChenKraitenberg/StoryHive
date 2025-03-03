package com.example.storyhive.ui.comment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.storyhive.R
import com.example.storyhive.data.models.Comment
import com.example.storyhive.databinding.ItemCommentBinding
import com.google.firebase.auth.FirebaseAuth

class CommentsAdapter(
    private var comments: List<Comment> = emptyList(),
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

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

        fun bind(comment: Comment) {
            binding.apply {
                // Set comment text and username
                commentText.text = comment.content
                userNameText.text = comment.userName

                // Format timestamp
                val formattedTime = android.text.format.DateFormat.format(
                    "MMM dd, yyyy HH:mm",
                    comment.timestamp
                )
                commentTimestamp.text = formattedTime.toString()

                // Set visibility of delete button based on user
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                deleteButton.visibility = if (currentUserId == comment.userId) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Set click listener for delete button
                deleteButton.setOnClickListener {
                    onDeleteClick(comment)
                }
            }
        }
    }
}