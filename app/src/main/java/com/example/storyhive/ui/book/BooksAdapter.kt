// BooksAdapter.kt
package com.example.storyhive.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.storyhive.R
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.models.Book
import com.example.storyhive.databinding.ItemBookBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Adapter for displaying a list of books in a RecyclerView.
 * Uses caching to efficiently load book cover images.
 * @param imageCacheManager Handles local image caching for better performance.
 * @param onBookClick Callback function triggered when a book is clicked.
 */
class BooksAdapter(
    private val imageCacheManager: ImageCacheManager,
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, BooksAdapter.BookViewHolder>(BookDiffCallback()) {


    // Coroutine scope for handling background tasks (image caching)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBookBinding.inflate(inflater, parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for book items.
     */
    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {


        /**
         * Binds the book data to the view components.
         * Loads book details including title, author, description, rating, and cover image.
         */
        fun bind(book: Book) {
            binding.apply {
                bookTitle.text = book.title
                bookAuthor.text = book.author
                bookDescription.text = book.description.ifEmpty { "No description available" }

                // Handle book cover image with caching
                if (book.coverUrl.isNotEmpty()) {
                    coroutineScope.launch {
                        val localPath = withContext(Dispatchers.IO) {
                            imageCacheManager.getLocalPathForUrl(book.coverUrl)
                        }

                        if (localPath != null) {
                            // Load from local cache
                            Picasso.get()
                                .load(File(localPath))
                                .placeholder(R.drawable.book_placeholder)
                                .error(R.drawable.book_placeholder)
                                .into(bookCover)
                        } else {
                            // Load from network
                            Picasso.get()
                                .load(book.coverUrl)
                                .placeholder(R.drawable.book_placeholder)
                                .error(R.drawable.book_placeholder)
                                .into(bookCover)

                            // Cache the image in background
                            coroutineScope.launch(Dispatchers.IO) {
                                imageCacheManager.cacheImage(book.coverUrl)
                            }
                        }
                    }
                } else {
                    // Use default placeholder if no cover image is available
                    bookCover.setImageResource(R.drawable.book_placeholder)
                }

                if (book.coverUrl.isNotEmpty()) {

                    val secureUrl = book.coverUrl.replace("http:", "https:")

                    Glide.with(itemView.context)
                        .load(secureUrl)
                        .placeholder(R.drawable.book_placeholder)
                        .error(R.drawable.book_placeholder)
                        .into(bookCover)
                } else {
                    // Use default placeholder if no cover image is available
                    bookCover.setImageResource(R.drawable.book_placeholder)
                }

                // Display book genre and rating
                bookGenre.text = book.genre
                //ratingBar.rating = book.rating
                //bookRating.text = book.rating.toString()

                // Set click listener for the book item
                root.setOnClickListener { onBookClick(book) }
            }
        }
    }


    /**
     * DiffUtil implementation for efficiently updating book lists.
     */
    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}