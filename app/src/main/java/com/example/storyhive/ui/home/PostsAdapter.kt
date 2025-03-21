package com.example.storyhive.ui.home

import StorageRepository
import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.storyhive.R
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.models.Post
import com.example.storyhive.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * Adapter for displaying a list of posts in a RecyclerView.
 * Handles UI interactions such as liking, commenting, editing, and deleting posts.
 */
class PostsAdapter(
    private val imageCacheManager: ImageCacheManager
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Click listeners for different actions on a post
    private var onLikeClickListener: ((Post) -> Unit)? = null
    private var onCommentClickListener: ((Post) -> Unit)? = null
    private var onEditClickListener: ((Post) -> Unit)? = null
    private var onDeleteClickListener: ((Post) -> Unit)? = null
    private var onCommentCountClickListener: ((Post) -> Unit)? = null

    /**
     * Sets a listener for when a post is liked.
     */
    fun setOnLikeClickListener(listener: (Post) -> Unit) {
        onLikeClickListener = listener
    }

    /**
     * Sets a listener for when the comment button is clicked.
     */
    fun setOnCommentClickListener(listener: (Post) -> Unit) {
        onCommentClickListener = listener
    }

    /**
     * Sets a listener for when the edit button is clicked.
     */
    fun setOnEditClickListener(listener: (Post) -> Unit) {
        onEditClickListener = listener
    }

    /**
     * Sets a listener for when the delete button is clicked.
     */
    fun setOnDeleteClickListener(listener: (Post) -> Unit) {
        onDeleteClickListener = listener
    }


    /**
     * Sets a listener for when the comment count is clicked.
     */
    fun setOnCommentCountClickListener(listener: (Post) -> Unit) {
        onCommentCountClickListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }


    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }


    /**
     * Callback for calculating the difference between old and new post items.
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timestamp

        return when {
            diff < 60000 -> "Just now" // פחות מדקה
            diff < 3600000 -> "${diff / 60000} minutes ago" // פחות משעה
            diff < 86400000 -> "${diff / 3600000} hours ago" // פחות מיום
            diff < 604800000 -> "${diff / 86400000} days ago" // פחות משבוע
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.apply {
                // --- User Information ---
                // Display username
                postAuthor.text = post.userDisplayName

                val formattedTime = formatTimestamp(post.timestamp)
                binding.postTimestamp.text = formattedTime

                // Set user initials for avatar fallback
                userInitials.text = getInitials(post.userDisplayName)

                // --- Book Information ---
                // Display book title
                postTitle.text = post.bookTitle

                // Display author if available
                if (post.bookAuthor.isNotEmpty()) {
                    bookAuthor.text = post.bookAuthor
                    bookAuthor.visibility = View.VISIBLE
                } else {
                    bookAuthor.visibility = View.GONE
                }

                // Display book description if available
                if (post.bookDescription.isNotEmpty()) {
                    postContent.text = post.bookDescription
                    postContent.visibility = View.VISIBLE
                } else {
                    postContent.visibility = View.GONE
                }

                if (!post.review.isNullOrEmpty()) {
                    postReview.text = post.review
                    postReview.visibility = View.VISIBLE
                } else {
                    postReview.visibility = View.GONE
                }

                // --- Book Cover Image ---
                if (!post.imageUrl.isNullOrEmpty()) {
                    coroutineScope.launch {
                        val localPath = withContext(Dispatchers.IO) {
                            imageCacheManager.getLocalPathForUrl(post.imageUrl!!)
                        }

                        if (localPath != null) {
                            // Load from local cache
                            Picasso.get()
                                .load(File(localPath))
                                .placeholder(R.drawable.ic_book_placeholder)
                                .into(bookCoverImage)

                            bookCoverImage.visibility = View.VISIBLE
                        } else {
                            // Load from network and cache in background
                            Picasso.get()
                                .load(post.imageUrl)
                                .placeholder(R.drawable.ic_book_placeholder)
                                .into(bookCoverImage)

                            bookCoverImage.visibility = View.VISIBLE

                            // Cache the image in background
                            coroutineScope.launch(Dispatchers.IO) {
                                imageCacheManager.cacheImage(post.imageUrl!!)
                            }
                        }
                    }
                } else if (!post.imageBase64.isNullOrEmpty()) {
                    try {
                        // Try to convert base64 string to bitmap
                        val bitmap = StorageRepository().decodeBase64ToBitmap(post.imageBase64!!)
                        bookCoverImage.setImageBitmap(bitmap)
                        bookCoverImage.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Log.e("PostsAdapter", "Failed to decode post image", e)
                        bookCoverImage.visibility = View.GONE
                    }
                } else {
                    bookCoverImage.visibility = View.GONE
                }

                // --- Profile Image ---
                if (post.userProfileImage.isNotEmpty()) {
                    coroutineScope.launch {
                        val localPath = withContext(Dispatchers.IO) {
                            imageCacheManager.getLocalPathForUrl(post.userProfileImage)
                        }

                        if (localPath != null) {
                            // Load from local cache
                            Picasso.get()
                                .load(File(localPath))
                                .placeholder(R.drawable.ic_user_placeholder)
                                .into(profileImage)

                            profileImage.visibility = View.VISIBLE
                            userInitials.visibility = View.GONE
                        } else {
                            // First check if it's a base64 string
                            try {
                                val bitmap = StorageRepository().decodeBase64ToBitmap(post.userProfileImage)
                                profileImage.setImageBitmap(bitmap)
                                profileImage.visibility = View.VISIBLE
                                userInitials.visibility = View.GONE
                            } catch (e: Exception) {
                                // If not a valid base64, try to load as URL
                                Picasso.get()
                                    .load(post.userProfileImage)
                                    .placeholder(R.drawable.ic_user_placeholder)
                                    .into(profileImage)

                                profileImage.visibility = View.VISIBLE
                                userInitials.visibility = View.GONE

                                // Cache the image in background
                                coroutineScope.launch(Dispatchers.IO) {
                                    imageCacheManager.cacheImage(post.userProfileImage)
                                }
                            }
                        }
                    }
                } else {
                    profileImage.visibility = View.GONE
                    userInitials.visibility = View.VISIBLE
                }


                // --- Interaction Controls ---
                // Setup like button and count
                setupLikeButton(post)

                // Update comment count
                commentCount.text = post.commentCount.toString()

                // Setup comment button
                commentButton.setOnClickListener {
                    onCommentClickListener?.invoke(post)
                }

                // Setup comment count click listener
                commentCount.setOnClickListener {

                    try {
                        onCommentCountClickListener?.invoke(post)
                    } catch (e: Exception) {
                        Log.e("PostsAdapter", "Error on comment count click", e)
                    }
                }


                // --- User Permissions ---
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isPostOwner = currentUserId == post.userId

                // Show edit button only for post owner
                editButton.visibility = if (isPostOwner) View.VISIBLE else View.GONE
                editButton.setOnClickListener {
                    onEditClickListener?.invoke(post)
                }

                // Show delete button only for post owner
                deleteButton.visibility = if (isPostOwner) View.VISIBLE else View.GONE
                deleteButton.setOnClickListener {
                    onDeleteClickListener?.invoke(post)
                }

            }
        }

        // Helper method to handle like button setup
        @SuppressLint("SetTextI18n")
        private fun setupLikeButton(post: Post) {
            // Update like count
            binding.likeCount.text = post.likes.toString()

            // Set like button color based on whether user has liked the post
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
            val isLiked = currentUserUid?.let { uid ->
                post.likedBy?.contains(uid) ?: false
            } ?: false

            binding.likeButton.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    if (isLiked) R.color.liked else R.color.text_secondary
                ),
                android.graphics.PorterDuff.Mode.SRC_IN
            )

            // Setup like button click animation and listener
            binding.likeButton.setOnClickListener {
                it.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(150)
                }
                onLikeClickListener?.invoke(post)
            }
        }
    }


    private fun getInitials(name: String): String {
        return name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

}

