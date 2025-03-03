package com.example.storyhive.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storyhive.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.example.storyhive.ui.comment.CommentDialogFragment

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var postsAdapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter().apply {
            setOnLikeClickListener { post ->
                viewModel.likePost(post.postId)
            }


            //edit click listener
            setOnEditClickListener { post ->
                // Navigate to edit post screen with the post as argument
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToEditPost(post)
                )
            }

            setOnDeleteClickListener { post ->  // ✅ הוספת מחיקת פוסט
                viewModel.deletePost(post.postId)
            }

            setOnCommentClickListener { post ->
                // Create and show the comment dialog with proper callback
                val dialog = CommentDialogFragment.newInstance(post.postId)
                dialog.setOnCommentAddedListener {
                    // This will be called when comment is successfully added
                    viewModel.refreshPosts()
                }
                dialog.show(childFragmentManager, "comment_dialog")
            }

            setOnCommentCountClickListener { post ->
                try {
                    // מעבר למסך התגובות עם ה-ID של הפוסט וכותרת הפוסט
                    val action = HomeFragmentDirections.actionHomeToComments(
                        postId = post.postId,
                        postTitle = post.bookTitle
                    )
                    if (isAdded && !isDetached) {
                        findNavController().navigate(action)
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error navigating to comments: ${e.message}", e)
                    context?.let {
                        Toast.makeText(it, "שגיאה בטעינת מסך התגובות", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.postsRecyclerView.apply {
            adapter = postsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }
    }


    private fun setupObservers() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            updateEmptyState(posts.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading && postsAdapter.itemCount == 0
            binding.swipeRefresh.isRefreshing = isLoading && postsAdapter.itemCount > 0
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        // ✅ מאזין לתוצאה של מחיקת פוסט ומציג הודעה מתאימה
        viewModel.deleteStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "הפוסט נמחק בהצלחה!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "שגיאה במחיקת הפוסט", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateText.isVisible = isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}