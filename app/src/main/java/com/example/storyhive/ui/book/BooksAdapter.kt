package com.example.storyhive.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.storyhive.R
import com.example.storyhive.data.models.Book
import com.example.storyhive.databinding.ItemBookBinding
import com.squareup.picasso.Picasso

class BooksAdapter(private val onBookClick: (Book) -> Unit) :
    ListAdapter<Book, BooksAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BooksAdapter.BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding, onBookClick)
    }

    override fun onBindViewHolder(holder: BooksAdapter.BookViewHolder, position: Int) {
        val currentBook = getItem(position)
        holder.bind(currentBook)
    }

    inner class BookViewHolder(
        private val binding: ItemBookBinding,
        private val onBookClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.apply {
                bookTitle.text = book.title
                bookAuthor.text = book.author
                bookDescription.text = book.description
                ratingBar.rating = book.rating

                Picasso.get()
                    .load(book.coverUrl)
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .into(bookCover)

                root.setOnClickListener { onBookClick(book) }
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem == newItem
    }
}