package com.example.storyhive.ui.book

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.storyhive.R
import com.example.storyhive.databinding.FragmentBookDetailBinding
import com.example.storyhive.data.models.Book

class BookDetailFragment : Fragment() {
    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BookDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val book = args.selectedBook

        // Display book info
        displayBookDetails(book)

        // Setup action buttons
        setupActionButtons(book)

        // Setup back button
        setupBackButton()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun displayBookDetails(book: Book) {
        binding.apply {
            bookTitleTextView.text = book.title
            bookAuthorTextView.text = book.author
            bookDescriptionTextView.text = book.description

            // Set additional details if available
            val details = StringBuilder()
            if (book.publishedDate.isNotEmpty()) {
                details.append("Published: ${book.publishedDate}")
            }
            if (book.pageCount > 0) {
                if (details.isNotEmpty()) details.append(" • ")
                details.append("Pages: ${book.pageCount}")
            }

            bookDetailsChip.text = details.toString()

            // Load book cover
            if (book.coverUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(book.coverUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .into(bookCoverImage)
            }
        }
    }

    private fun setupActionButtons(book: Book) {
        // Prepare search query for external links
        val searchQuery = "${book.title} ${book.author}"

        // Setup Kindle button
        binding.readOnKindleButton.setOnClickListener {
            openExternalLink("https://www.amazon.com/kindle-dbs/search?query=${Uri.encode(searchQuery)}")
        }

        // Setup Audible button
        binding.listenOnAudibleButton.setOnClickListener {
            openExternalLink("https://www.audible.com/search?keywords=${Uri.encode(searchQuery)}")
        }

        // Setup Goodreads button
        binding.viewOnGoodreadsButton.setOnClickListener {
            openExternalLink("https://www.goodreads.com/search?q=${Uri.encode(searchQuery)}")
        }
    }

    private fun openExternalLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Handle exception (e.g., no browser available)
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}