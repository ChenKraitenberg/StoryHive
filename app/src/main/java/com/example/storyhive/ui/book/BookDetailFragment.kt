package com.example.storyhive.ui.book

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.storyhive.R
import com.example.storyhive.StoryHiveApplication
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.local.StoryHiveDatabase
import com.example.storyhive.data.util.Resource
import com.example.storyhive.databinding.FragmentBookDetailBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File


/**
 * **BookDetailFragment** - Displays details of a selected book.
 * - Fetches book details from ViewModel and displays them.
 * - Handles caching for offline support.
 * - Provides navigation and interaction options.
 */
class BookDetailFragment : Fragment() {
    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BookDetailFragmentArgs by navArgs()
    private val viewModel: BookDetailViewModel by viewModels()
    private lateinit var imageCacheManager: ImageCacheManager

    /**
     * Inflates the fragment layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }


    /**
     * Called after the view is created. Initializes UI components.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initImageCacheManager()
        setupBackButton()
        setupFlowCollectors()

        // Start loading book details
        viewModel.loadBookDetails(args.selectedBook.id)
    }

    /**
     * Initializes the image caching manager.
     * Uses the application instance if available, otherwise creates a new one.
     */
    private fun initImageCacheManager() {
        imageCacheManager = try {
            (requireActivity().application as StoryHiveApplication).imageCacheManager
        } catch (e: Exception) {
            val database = StoryHiveDatabase.getInstance(requireContext())
            ImageCacheManager.getInstance(requireContext(), database.imageCacheDao())
        }
    }

    /**
     * Configures the back button navigation.
     */
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Observes data from ViewModel and updates the UI accordingly.
     */
    private fun setupFlowCollectors() {
        // Collect book details flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookDetails.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // No special UI handling needed for loading state
                        }
                        is Resource.Success -> {
                            resource.data?.let { bookDetails ->
                                displayBookDetails(bookDetails)
                                setupActionButtons(bookDetails.id, bookDetails.title, bookDetails.author)
                            }
                        }
                        is Resource.Error -> {
                            resource.data?.let { bookDetails ->
                                displayBookDetails(bookDetails)
                                setupActionButtons(bookDetails.id, bookDetails.title, bookDetails.author)

                                // Show a warning if data is outdated
                                Toast.makeText(requireContext(),
                                    "Displaying local data ${resource.message}",
                                    Toast.LENGTH_LONG).show()
                            } ?: run {
                                // No data at all, show an error message
                                Toast.makeText(requireContext(),
                                    resource.message ?: "Error loading book details",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        // Collect related books flow (optional)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.relatedBooks.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                        }
                        is Resource.Success -> {
                            resource.data?.let { books ->
                                if (books.isNotEmpty()) {
                                }
                            }
                        }
                        is Resource.Error -> {
                            if (resource.data?.isNotEmpty() == true) {
                                // Display cached related books
                                // relatedBooksAdapter.submitList(resource.data)
                            } else {
                                // Show error message if no data available
                                Toast.makeText(requireContext(),
                                    resource.message ?: "Error loading related books",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Displays the details of the selected book, including title, author, description, and cover image.
     * Uses caching for optimized image loading and error handling.
     */
    private fun displayBookDetails(bookDetails: BookDetailsState) {
        binding.apply {
            // Set book title, author, and description
            bookTitleTextView.text = bookDetails.title
            bookAuthorTextView.text = bookDetails.author
            bookDescriptionTextView.text = bookDetails.description

            // Format additional details (published date & page count)
            val details = StringBuilder()
            if (bookDetails.publishedDate.isNotEmpty()) {
                details.append("Published: ${bookDetails.publishedDate}")
            }
            if (bookDetails.pageCount > 0) {
                if (details.isNotEmpty()) details.append(" • ")
                details.append("Pages: ${bookDetails.pageCount}")
            }

            bookDetailsChip.text = details.toString()

            // Load book cover image with caching
            bookDetails.coverImageUrl?.let { coverUrl ->
                if (coverUrl.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            Log.d("BookDetailFragment", "Loading image from URL: $coverUrl")

                            // Convert "http" to "https" for security reasons
                            val secureUrl = coverUrl.replace("http:", "https:")

                            // Check if the image is already cached locally
                            val localPath = imageCacheManager.getLocalPathForUrl(secureUrl)

                            if (localPath != null) {
                                // Load the image from local cache
                                Log.d("BookDetailFragment", "Loading from local cache: $localPath")
                                Picasso.get()
                                    .load(File(localPath))
                                    .placeholder(R.drawable.ic_book_placeholder)
                                    .error(R.drawable.ic_book_placeholder)
                                    .into(bookCoverImage)
                            } else {
                                // Load the image from network
                                Log.d("BookDetailFragment", "Loading from network: $secureUrl")
                                Picasso.get()
                                    .load(secureUrl)
                                    .placeholder(R.drawable.ic_book_placeholder)
                                    .error(R.drawable.ic_book_placeholder)
                                    .into(bookCoverImage)

                                // Cache the image in the background for future use
                                lifecycleScope.launch {
                                    try {
                                        imageCacheManager.cacheImage(secureUrl)
                                    } catch (e: Exception) {
                                        Log.e("BookDetailFragment", "Failed to cache image", e)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BookDetailFragment", "Error loading image", e)
                            // If an error occurs, display the default placeholder image
                            bookCoverImage.setImageResource(R.drawable.ic_book_placeholder)
                        }
                    }
                } else {
                    // No image URL provided, display the default placeholder
                    bookCoverImage.setImageResource(R.drawable.ic_book_placeholder)
                }
            } ?: run {
                // If coverImageUrl is null, display the default placeholder
                bookCoverImage.setImageResource(R.drawable.ic_book_placeholder)
            }
        }
    }

    /**
     * Configures action buttons that allow users to explore the book on external platforms
     * such as Amazon Kindle, Audible, and Goodreads.
     *
     * @param bookId The unique identifier of the book.
     * @param title The title of the book.
     * @param author The author of the book.
     */
    private fun setupActionButtons(bookId: String, title: String, author: String) {
        // Construct a search query using the book title and author
        val searchQuery = "$title $author"

        // Setup the "Read on Kindle" button to open the book's page on Amazon Kindle
        binding.readOnKindleButton.setOnClickListener {
            openExternalLink("https://www.amazon.com/kindle-dbs/search?query=${Uri.encode(searchQuery)}")
        }

        // Setup the "Listen on Audible" button to search for the book on Audible
        binding.listenOnAudibleButton.setOnClickListener {
            openExternalLink("https://www.audible.com/search?keywords=${Uri.encode(searchQuery)}")
        }

        // Setup the "View on Goodreads" button to find the book on Goodreads
        binding.viewOnGoodreadsButton.setOnClickListener {
            openExternalLink("https://www.goodreads.com/search?q=${Uri.encode(searchQuery)}")
        }
    }

    /**
     * Opens an external web link using the default browser.
     * Handles potential exceptions such as the absence of a browser on the device.
     * @param url The URL to open in an external browser.
     */
    private fun openExternalLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Handle exceptions (e.g., no browser available on the device)
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Ensures proper cleanup of the binding object to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}