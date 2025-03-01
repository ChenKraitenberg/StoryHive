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
import com.example.storyhive.data.models.Book
import com.example.storyhive.databinding.FragmentSearchBinding
import com.example.storyhive.ui.book.BooksAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var booksAdapter: BooksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

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


        binding.searchResultsRecyclerView.apply {
            adapter = booksAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    @SuppressLint("ServiceCast")
    private fun setupSearchView() {
        // שימוש ב-debounce למניעת בקשות מיותרות
        var searchJob: Job? = null

        binding.searchEditText.addTextChangedListener { text ->
            searchJob?.cancel()

            // חפש רק אם המחרוזת מכילה לפחות 3 תווים
            val query = text?.toString() ?: ""
            if (query.length >= 3) {
                searchJob = lifecycleScope.launch {
                    delay(500) // המתן 500 מילישניות לפני חיפוש
                    viewModel.searchBooks(query)
                }
            } else if (query.isEmpty()) {
                // נקה את התוצאות כשהחיפוש ריק
                viewModel.searchBooks("")
            }
        }

        // הגדר לחיצה על כפתור החיפוש במקלדת
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text?.toString() ?: ""
                viewModel.searchBooks(query)

                // הסתר את המקלדת
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                return@setOnEditorActionListener true
            }
            false
        }
    }


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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}