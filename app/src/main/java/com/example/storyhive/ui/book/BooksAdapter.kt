// BooksAdapter.kt
package com.example.storyhive.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.storyhive.R
import com.example.storyhive.data.models.Book
import com.example.storyhive.databinding.ItemBookBinding
import com.squareup.picasso.Picasso

class BooksAdapter(private val onBookClick: (Book) -> Unit) :
    ListAdapter<Book, BooksAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBookBinding.inflate(inflater, parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.apply {
                bookTitle.text = book.title
                bookAuthor.text = book.author
                bookDescription.text = book.description.ifEmpty { "No description available" }

                // כאן אנחנו מטפלים בתמונה
                if (book.coverUrl.isNotEmpty()) {
                    // וודא שהקישור מתחיל ב-https
                    val secureUrl = book.coverUrl.replace("http:", "https:")

                    Glide.with(itemView.context)
                        .load(secureUrl)
                        .placeholder(R.drawable.book_placeholder)
                        .error(R.drawable.book_placeholder)
                        .into(bookCover)
                } else {
                    bookCover.setImageResource(R.drawable.book_placeholder)
                }

                // טיפול בז'אנר ודירוג
                bookGenre.text = book.genre
                ratingBar.rating = book.rating
                bookRating.text = book.rating.toString()

                // הגדרת אירוע לחיצה
                root.setOnClickListener { onBookClick(book) }
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}