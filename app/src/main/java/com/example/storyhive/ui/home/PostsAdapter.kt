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
import com.bumptech.glide.Glide
import com.example.storyhive.R
import com.example.storyhive.data.models.Post
import com.example.storyhive.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
class PostsAdapter : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private var onLikeClickListener: ((Post) -> Unit)? = null
    private var onCommentClickListener: ((Post) -> Unit)? = null
    private var onEditClickListener: ((Post) -> Unit)? = null
    private var onDeleteClickListener: ((Post) -> Unit)? = null


    fun setOnLikeClickListener(listener: (Post) -> Unit) {
        onLikeClickListener = listener
    }

    fun setOnCommentClickListener(listener: (Post) -> Unit) {
        onCommentClickListener = listener
    }

    fun setOnEditClickListener(listener: (Post) -> Unit) {
        onEditClickListener = listener
    }

    fun setOnDeleteClickListener(listener: (Post) -> Unit) {
        onDeleteClickListener = listener
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


    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.apply {
                // --- User Information ---
                // Display username
                postAuthor.text = post.userDisplayName

                // Display profile image
                setupProfileImage(post.userDisplayName, post.userProfileImage)


                // הצגת תמונת פרופיל
                if (post.userProfileImage.isNotEmpty()) {
                    try {
                        // נסה להמיר את ה-base64 string לbitmap
                        val bitmap = StorageRepository().decodeBase64ToBitmap(post.userProfileImage)
                        binding.profileImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        Log.e("PostsAdapter", "Failed to decode user profile image", e)
                        binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
                    }
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
                }
                Glide.with(itemView.context)
                    .load(post.userProfileImage)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(binding.profileImage)


                // הצגת שם הספר (כותרת)
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

                // Display review if available
                if (!post.review.isNullOrEmpty()) {
                    postReview.text = post.review
                    postReview.visibility = View.VISIBLE
                } else {
                    postReview.visibility = View.GONE
                }

                // --- Book Cover Image ---
                setupBookCoverImage(post)

                // --- Interaction Controls ---
                // Setup like button and count
                setupLikeButton(post)

                // טיפול בתגובות - עדכון ספירת תגובות
                commentCount.text = post.commentCount.toString()


                // Setup comment button
                commentButton.setOnClickListener {
                    onCommentClickListener?.invoke(post)
                }

                // --- User Permissions ---
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isPostOwner = currentUserId == post.userId

                // Show edit button only for post owner
                editButton.visibility = if (isPostOwner) View.VISIBLE else View.GONE
                editButton.setOnClickListener {
                    onEditClickListener?.invoke(post)
                }

                // Set on edit click listener
                editButton.setOnClickListener {
                    onEditClickListener?.invoke(post)
                }
                
                // ✅ הצגת כפתור מחיקה רק ליוצר הפוסט
                val isAuthor = post.userId == currentUserUid
                deleteButton.visibility = if (isAuthor) View.VISIBLE else View.GONE


                // Show delete button only for post owner
                deleteButton.visibility = if (isPostOwner) View.VISIBLE else View.GONE
                deleteButton.setOnClickListener {
                    onDeleteClickListener?.invoke(post)
                }
            }
        }

        private fun getInitials(name: String): String {
            return name.split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
        }

        // Update the setupProfileImage method
        private fun setupProfileImage(userName: String, userProfile: String?) {
            // Set user initials in the circular initial view
            binding.userInitials.text = getInitials(userName)

            if (!userProfile.isNullOrEmpty()) {
                if (userProfile.startsWith("http")) {
                    // Load with Glide if it's a URL
                    Glide.with(itemView.context)
                        .load(userProfile)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(binding.profileImage)
                    binding.profileImage.visibility = View.VISIBLE
                    binding.userInitials.visibility = View.GONE
                } else {
                    // Try to decode as Base64
                    try {
                        val bitmap = StorageRepository().decodeBase64ToBitmap(userProfile)
                        binding.profileImage.setImageBitmap(bitmap)
                        binding.profileImage.visibility = View.VISIBLE
                        binding.userInitials.visibility = View.GONE
                    } catch (e: Exception) {
                        Log.e("PostsAdapter", "Failed to decode profile image", e)
                        binding.profileImage.visibility = View.GONE
                        binding.userInitials.visibility = View.VISIBLE
                    }
                }
            } else {
                binding.profileImage.visibility = View.GONE
                binding.userInitials.visibility = View.VISIBLE
            }
        }

        // Helper method to handle book cover image display
        private fun setupBookCoverImage(post: Post) {
            // Check if we have Base64 image data
            if (!post.imageBase64.isNullOrEmpty()) {
                try {
                    // Display Base64 image
                    val bitmap = StorageRepository().decodeBase64ToBitmap(post.imageBase64!!)
                    binding.bookCoverImage.setImageBitmap(bitmap)
                    binding.bookCoverImage.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e("PostsAdapter", "Failed to decode post image", e)
                    checkForImageUrl(post)
                }
            } else {
                // If no Base64 data, check for image URL
                checkForImageUrl(post)
            }
        }

        // Helper method to check for image URL
        private fun checkForImageUrl(post: Post) {
            if (!post.imageUrl.isNullOrEmpty()) {
                // Display image from URL
                Glide.with(itemView.context)
                    .load(post.imageUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .into(binding.bookCoverImage)
                binding.bookCoverImage.visibility = View.VISIBLE
            } else {
                // No image available
                binding.bookCoverImage.visibility = View.GONE
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

}
