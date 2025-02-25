package com.example.storyhive.ui.book

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.storyhive.R
import com.example.storyhive.databinding.FragmentBookDetailBinding
import com.example.storyhive.ui.search.ReviewAdapter
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

//class BookDetailFragment : Fragment() {
//    private val viewModel: BookDetailViewModel by viewModels()
//    private var _binding: FragmentBookDetailBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val bookId = arguments?.getString("BOOK_ID") ?: return
//
//        viewModel.loadBookDetails(bookId)
//
//        viewModel.bookDetails.observe(viewLifecycleOwner) { bookDetails ->
//            bookDetails?.let {
//                binding.bookTitleTextView.text = it.title
//                binding.bookAuthorTextView.text = it.author
//                binding.bookDescriptionTextView.text = it.description
//                binding.bookDetailsTextView.text = "${it.publishedDate} | ${it.pageCount} pages"
//
//                // Load book cover image
//                it.coverImageUrl?.let { imageUrl ->
//                    Glide.with(requireContext())
//                        .load(imageUrl)
//                        .placeholder(R.drawable.ic_book_placeholder)
//                        .into(binding.bookCoverImage)
//                }
//            }
//        }
//    }
//}
class BookDetailFragment : Fragment() {
    private val bookDetailViewModel: BookDetailViewModel by viewModels()
    private val bookRatingViewModel: BookRatingViewModel by viewModels()
    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookId = arguments?.getString("BOOK_ID") ?: return

        // הכנת RecyclerView לביקורות
        reviewAdapter = ReviewAdapter(emptyList())
        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reviewAdapter
        }

        // טעינת פרטי ספר
        bookDetailViewModel.loadBookDetails(bookId)
        bookDetailViewModel.bookDetails.observe(viewLifecycleOwner) { bookDetails ->
            bookDetails?.let {
                binding.bookTitleTextView.text = it.title // הוסף שורה זו
                binding.bookAuthorTextView.text = it.author
                binding.bookDescriptionTextView.text = it.description
                binding.bookDetailsTextView.text = "${it.publishedDate} | ${it.pageCount} pages"

                // טעינת תמונת כריכה
                it.coverImageUrl?.let { imageUrl ->
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .into(binding.bookCoverImage)
                }
            }
        }

        // טעינת דירוגים וביקורות
        bookRatingViewModel.fetchBookRatings(bookId)
        bookRatingViewModel.bookRatings.observe(viewLifecycleOwner) { ratingData ->
            binding.apply {
                bookRatingBar.rating = ratingData.averageRating
                bookRatingTextView.text = "${String.format("%.1f", ratingData.averageRating)} (${ratingData.totalRatings} ratings)"

                // עדכון רשימת הביקורות
                reviewAdapter.updateReviews(ratingData.reviews)
            }
        }

        // הוספת ביקורת
        binding.addReviewButton.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                showAddReviewDialog(bookId)
            } else {
                // הפניה למסך התחברות
                findNavController().navigate(R.id.action_to_login)
            }
        }
    }

    private fun showAddReviewDialog(bookId: String) {
        val dialogFragment = AddReviewDialogFragment().apply {
            arguments = Bundle().apply {
                putString("BOOK_ID", bookId)
            }
        }
        dialogFragment.show(childFragmentManager, "add_review")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}