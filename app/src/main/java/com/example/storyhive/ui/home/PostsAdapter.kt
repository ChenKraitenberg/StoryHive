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
<<<<<<< HEAD
import com.example.storyhive.R
import com.example.storyhive.data.local.ImageCacheManager
=======
import com.bumptech.glide.Glide
import com.example.storyhive.R
>>>>>>> main
import com.example.storyhive.data.models.Post
import com.example.storyhive.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
<<<<<<< HEAD
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Adapter for displaying a list of posts in a RecyclerView.
 * Handles UI interactions such as liking, commenting, editing, and deleting posts.
 */
class PostsAdapter(
    private val imageCacheManager: ImageCacheManager
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Click listeners for different actions on a post
=======
class PostsAdapter : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

>>>>>>> main
    private var onLikeClickListener: ((Post) -> Unit)? = null
    private var onCommentClickListener: ((Post) -> Unit)? = null
    private var onEditClickListener: ((Post) -> Unit)? = null
    private var onDeleteClickListener: ((Post) -> Unit)? = null
    private var onCommentCountClickListener: ((Post) -> Unit)? = null

<<<<<<< HEAD
    /**
     * Sets a listener for when a post is liked.
     */
=======

>>>>>>> main
    fun setOnLikeClickListener(listener: (Post) -> Unit) {
        onLikeClickListener = listener
    }

<<<<<<< HEAD
    /**
     * Sets a listener for when the comment button is clicked.
     */
=======
>>>>>>> main
    fun setOnCommentClickListener(listener: (Post) -> Unit) {
        onCommentClickListener = listener
    }

<<<<<<< HEAD
    /**
     * Sets a listener for when the edit button is clicked.
     */
=======
>>>>>>> main
    fun setOnEditClickListener(listener: (Post) -> Unit) {
        onEditClickListener = listener
    }

<<<<<<< HEAD
    /**
     * Sets a listener for when the delete button is clicked.
     */
=======
>>>>>>> main
    fun setOnDeleteClickListener(listener: (Post) -> Unit) {
        onDeleteClickListener = listener
    }

<<<<<<< HEAD
    /**
     * Sets a listener for when the comment count is clicked.
     */
=======
>>>>>>> main
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

<<<<<<< HEAD
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }


    /**
     * Callback for calculating the difference between old and new post items.
     */
=======


    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
    val post = getItem(position)
    holder.bind(post)
    }


>>>>>>> main
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

<<<<<<< HEAD
                // Set user initials for avatar fallback
                userInitials.text = getInitials(post.userDisplayName)

=======
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
>>>>>>> main
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
<<<<<<< HEAD
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
=======
                setupBookCoverImage(post)
>>>>>>> main

                // --- Interaction Controls ---
                // Setup like button and count
                setupLikeButton(post)

<<<<<<< HEAD
                // Update comment count
                commentCount.text = post.commentCount.toString()

=======
                // טיפול בתגובות - עדכון ספירת תגובות
                commentCount.text = post.commentCount.toString()


>>>>>>> main
                // Setup comment button
                commentButton.setOnClickListener {
                    onCommentClickListener?.invoke(post)
                }

<<<<<<< HEAD
                // Setup comment count click listener
                commentCount.setOnClickListener {
=======

                //bind listener for click on comments count
                binding.commentCount.setOnClickListener {
>>>>>>> main
                    try {
                        onCommentCountClickListener?.invoke(post)
                    } catch (e: Exception) {
                        Log.e("PostsAdapter", "Error on comment count click", e)
                    }
                }

<<<<<<< HEAD
=======

>>>>>>> main
                // --- User Permissions ---
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isPostOwner = currentUserId == post.userId

                // Show edit button only for post owner
                editButton.visibility = if (isPostOwner) View.VISIBLE else View.GONE
                editButton.setOnClickListener {
                    onEditClickListener?.invoke(post)
                }

<<<<<<< HEAD
=======
                // Set on edit click listener
                editButton.setOnClickListener {
                    onEditClickListener?.invoke(post)
                }
                



>>>>>>> main
                // Show delete button only for post owner
                deleteButton.visibility = if (isPostOwner) View.VISIBLE else View.GONE
                deleteButton.setOnClickListener {
                    onDeleteClickListener?.invoke(post)
                }
<<<<<<< HEAD


            }
        }
=======
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
            // הגדר תמיד אותיות ראשונות
            binding.userInitials.text = getInitials(userName)

            // נקה ובדוק תמונה
            val cleanProfileImage = userProfile?.trim()

            when {
                // אם יש כתובת URL תקינה
                !cleanProfileImage.isNullOrEmpty() && cleanProfileImage.startsWith("http") -> {
                    Glide.with(itemView.context)
                        .load(cleanProfileImage)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(binding.profileImage)
                    binding.profileImage.visibility = View.VISIBLE
                    binding.userInitials.visibility = View.GONE
                }

                // נסה להמיר Base64
                !cleanProfileImage.isNullOrEmpty() -> {
                    val bitmap = StorageRepository().decodeBase64ToBitmap(cleanProfileImage)
                    if (bitmap != null) {
                        binding.profileImage.setImageBitmap(bitmap)
                        binding.profileImage.visibility = View.VISIBLE
                        binding.userInitials.visibility = View.GONE
                    } else {
                        // אם ההמרה נכשלה, הראה אותיות ראשונות
                        binding.profileImage.visibility = View.GONE
                        binding.userInitials.visibility = View.VISIBLE
                    }
                }

                // אחרת, הראה אותיות ראשונות
                else -> {
                    binding.profileImage.visibility = View.GONE
                    binding.userInitials.visibility = View.VISIBLE
                }
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

>>>>>>> main
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

<<<<<<< HEAD
    private fun getInitials(name: String): String {
        return name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }




}
=======
}
>>>>>>> main
