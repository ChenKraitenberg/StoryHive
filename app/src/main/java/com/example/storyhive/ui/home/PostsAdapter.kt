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

    fun setOnLikeClickListener(listener: (Post) -> Unit) {
        onLikeClickListener = listener
    }

    fun setOnCommentClickListener(listener: (Post) -> Unit) {
        onCommentClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }


//override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
//    val post = getItem(position)
//    holder.bind(post)
//}
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
                // הצגת שם המשתמש
                postAuthor.text = post.userDisplayName

                // הצגת תמונת פרופיל
//                if (post.userProfileImage.isNotEmpty()) {
//                    Picasso.get().load(post.userProfileImage).placeholder(R.drawable.ic_user_placeholder).into(profileImage)
//                } else {
//                    profileImage.setImageResource(R.drawable.ic_user_placeholder)
//                }
                // הצגת תמונת פרופיל
// הצגת תמונת פרופיל - שנה את הקוד הקיים
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
                postTitle.text = post.bookTitle

                // הצגת שם הסופר
                if (post.bookAuthor.isNotEmpty()) {
                    bookAuthor.text = post.bookAuthor
                    bookAuthor.visibility = View.VISIBLE
                } else {
                    bookAuthor.visibility = View.GONE
                }

                // הצגת תיאור הספר (אם יש)
                if (post.bookDescription.isNotEmpty()) {
                    postContent.text = post.bookDescription
                    postContent.visibility = View.VISIBLE
                } else {
                    postContent.visibility = View.GONE
                }

                // הצגת ביקורת (אם קיימת)
                if (!post.review.isNullOrEmpty()) {
                    postReview.text = post.review  // התייחסות ישירה - ללא תוספת "ביקורת:"
                    postReview.visibility = View.VISIBLE
                } else {
                    postReview.visibility = View.GONE
                }

                // שאר הקוד לטיפול בלייקים ותגובות
                likeCount.text = post.likes.toString()

                // טיפול בסטטוס לייק
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                val isLiked = currentUserUid?.let { uid ->
                    post.likedBy?.contains(uid) ?: false
                } ?: false

                likeButton.setColorFilter(
                    ContextCompat.getColor(itemView.context, if (isLiked) R.color.liked else R.color.text_secondary),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )

                likeButton.setOnClickListener {
                    it.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(150)
                    }
                    onLikeClickListener?.invoke(post)
                }

                commentButton.setOnClickListener {
                    onCommentClickListener?.invoke(post)
                }
            }
        }
    }

}
