// searchFrafment.kt
package com.example.storyhive.ui.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
<<<<<<< HEAD
import com.example.storyhive.StoryHiveApplication
import com.example.storyhive.data.models.Book
import com.example.storyhive.data.util.Resource
=======
import com.example.storyhive.data.models.Book
>>>>>>> main
import com.example.storyhive.databinding.FragmentSearchBinding
import com.example.storyhive.ui.book.BooksAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

<<<<<<< HEAD

class SearchFragment : Fragment() {
    // ViewBinding instance to access UI elements
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // ViewModel to handle search logic
    private val viewModel: SearchViewModel by viewModels()

    // Adapter for displaying book search results
    private lateinit var booksAdapter: BooksAdapter


    /**
     * Called when the fragment's view is created.
     * Inflates the layout using ViewBinding.
     */
=======
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var booksAdapter: BooksAdapter

>>>>>>> main
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

<<<<<<< HEAD
    /**
     * Called after the view has been created.
     * Initializes UI components and sets up event listeners.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()  // Initialize RecyclerView for search results
        setupSearchView()     // Configure search input field and behavior
        observeViewModel()    // Observe search results from ViewModel
    }

    /**
     * Initializes the RecyclerView for displaying search results.
     */
    private fun setupRecyclerView() {
        val imageCacheManager = (requireActivity().application as StoryHiveApplication).imageCacheManager

        booksAdapter = BooksAdapter(imageCacheManager) { book ->
            val action = SearchFragmentDirections.actionSearchToBookDetail(book)
            findNavController().navigate(action)
        }

=======
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        booksAdapter = BooksAdapter { book ->
            // ניווט לפרטי הספר כשלוחצים עליו
//            findNavController().navigate(
//                SearchFragmentDirections.actionSearchToBookDetail(book.id)
//            )

            val action = SearchFragmentDirections.actionSearchToBookDetail(book)
            findNavController().navigate(action)

        }


>>>>>>> main
        binding.searchResultsRecyclerView.apply {
            adapter = booksAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    @SuppressLint("ServiceCast")
    private fun setupSearchView() {
<<<<<<< HEAD
        // Use debounce to prevent unnecessary search requests
=======
        // שימוש ב-debounce למניעת בקשות מיותרות
>>>>>>> main
        var searchJob: Job? = null

        binding.searchEditText.addTextChangedListener { text ->
            searchJob?.cancel()

<<<<<<< HEAD
            // Search only if the query contains at least 3 characters
            val query = text?.toString() ?: ""
            if (query.length >= 3) {
                searchJob = lifecycleScope.launch {
                    delay(500) // Wait 500ms before triggering the search
                    viewModel.searchBooks(query)
                }
            } else if (query.isEmpty()) {
                // Clear search results when input is empty
=======
            // חפש רק אם המחרוזת מכילה לפחות 3 תווים
            val query = text?.toString() ?: ""
            if (query.length >= 3) {
                searchJob = lifecycleScope.launch {
                    delay(500) // המתן 500 מילישניות לפני חיפוש
                    viewModel.searchBooks(query)
                }
            } else if (query.isEmpty()) {
                // נקה את התוצאות כשהחיפוש ריק
>>>>>>> main
                viewModel.searchBooks("")
            }
        }

<<<<<<< HEAD
        // Handle search action when the keyboard's search button is pressed
=======
        // הגדר לחיצה על כפתור החיפוש במקלדת
>>>>>>> main
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text?.toString() ?: ""
                viewModel.searchBooks(query)

<<<<<<< HEAD
                // Hide the keyboard
=======
                // הסתר את המקלדת
>>>>>>> main
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                return@setOnEditorActionListener true
            }
            false
        }
    }


<<<<<<< HEAD
    /**
     * Observes the ViewModel's search results and updates the UI accordingly.
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyStateContainer.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE

                        // Use safe call with orEmpty() to prevent null issues
                        booksAdapter.submitList(resource.data.orEmpty())

                        binding.emptyStateContainer.visibility =
                            if (resource.data.isNullOrEmpty() && binding.searchEditText.text?.isNotEmpty() == true)
                                View.VISIBLE else View.GONE
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        // Display an error message if needed
                        booksAdapter.submitList(emptyList())
                        binding.emptyStateContainer.visibility = View.VISIBLE
                    }
                }
            }
=======
    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { books ->
            booksAdapter.submitList(books)

            // עדכון מצב המסך הריק
            binding.emptyStateContainer.visibility =
                if (books.isEmpty() && binding.searchEditText.text?.isNotEmpty() == true)
                    View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // הצג או הסתר מחוון טעינה
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
>>>>>>> main
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}