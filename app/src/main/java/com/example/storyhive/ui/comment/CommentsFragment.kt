package com.example.storyhive.ui.comment

import android.os.Bundle
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
import kotlinx.coroutines.launch

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
        // Set the post title
        binding.postTitleText.text = args.postTitle

        // Initialize the adapter
        commentsAdapter = CommentsAdapter(onDeleteClick = { comment ->
            deleteComment(comment.commentId, args.postId)
        })

        // Setup RecyclerView
        binding.commentsRecyclerView.apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Setup add comment button
        binding.addCommentButton.setOnClickListener {
            val commentText = binding.commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            } else {
                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadComments() {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE

        // Observe comments from repository
        repository.observeComments(args.postId) { comments ->
            binding.progressBar.visibility = View.GONE
            commentsAdapter.updateComments(comments)

            // Show empty state message if there are no comments
            if (comments.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
            } else {
                binding.emptyStateText.visibility = View.GONE
            }
        }
    }

    private fun addComment(content: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.addCommentButton.isEnabled = false
                binding.commentEditText.isEnabled = false

                repository.addComment(args.postId, content)

                // Clear input field after successful comment
                binding.commentEditText.text.clear()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to add comment", Toast.LENGTH_SHORT).show()
            } finally {
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