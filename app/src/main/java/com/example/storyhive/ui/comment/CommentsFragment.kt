package com.example.storyhive.ui.comment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storyhive.databinding.FragmentCommentsBinding
import com.example.storyhive.repository.PostRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout



/**
 * Fragment responsible for displaying and managing comments on a specific post.
 */
class CommentsFragment : Fragment() {
    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private val args: CommentsFragmentArgs by navArgs()
    private val repository = PostRepository()
    private lateinit var commentsAdapter: CommentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadComments()
    }

    private fun setupUI() {
        // Set the post title in the UI
        binding.postTitleText.text = args.postTitle

        // Initialize the comments adapter with a delete action
        commentsAdapter = CommentsAdapter(onDeleteClick = { comment ->
            deleteComment(comment.commentId, args.postId)
        })


        // Set up RecyclerView for displaying comments
        binding.commentsRecyclerView.apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }


        // Handle the add comment button click event
        binding.addCommentButton.setOnClickListener {
            val commentText = binding.commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            } else {
                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }


        // Handle the back button click event
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadComments() {
        // Ensure binding is not null before accessing views
        _binding?.let { binding ->
            binding.progressBar.visibility = View.VISIBLE

            // Observe comments from the repository and update the UI
            repository.observeComments(args.postId) { comments ->
                // Ensure the fragment is still attached before updating UI
                if (isAdded && !isDetached) {
                    binding.progressBar.visibility = View.GONE
                    commentsAdapter.updateComments(comments)

                    // Show empty state message if there are no comments
                    binding.emptyStateText.visibility =
                        if (comments.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun addComment(content: String) {
        lifecycleScope.launch {
            try {
                // Show loading state
                binding.progressBar.visibility = View.VISIBLE
                binding.addCommentButton.isEnabled = false
                binding.commentEditText.isEnabled = false

                // Set a timeout to prevent long wait times
                withTimeout(10000) {
                    repository.addComment(args.postId, content)
                }
                // Clear input field after a successful comment submission
                binding.commentEditText.text.clear()
                Toast.makeText(requireContext(), "Comment added successfully", Toast.LENGTH_SHORT).show()
            } catch (e: TimeoutCancellationException) {
                Toast.makeText(requireContext(), "Operation timed out", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CommentDialog", "Error adding comment", e)
                Toast.makeText(requireContext(), "Failed to add comment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                // Restore UI state after operation completion
                binding.progressBar.visibility = View.GONE
                binding.addCommentButton.isEnabled = true
                binding.commentEditText.isEnabled = true
            }
        }
    }

    private fun deleteComment(commentId: String, postId: String) {
        lifecycleScope.launch {
            try {
                repository.deleteComment(postId, commentId)
                Toast.makeText(requireContext(), "Comment deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}