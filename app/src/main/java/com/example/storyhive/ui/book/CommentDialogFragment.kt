package com.example.storyhive.ui.book

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.storyhive.databinding.DialogCommentBinding
import com.example.storyhive.repository.PostRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch



/**
 * A dialog fragment that allows users to add a comment to a post.
 * The comment is submitted asynchronously to Firestore via PostRepository.
 */
class CommentDialogFragment : DialogFragment() {

    private var _binding: DialogCommentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCommentBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Comment")
            .setView(binding.root)
            .setPositiveButton("Post") { _, _ ->
                val comment = binding.commentInput.text.toString()
                if (comment.isNotBlank()) {
                    lifecycleScope.launch {
                        val postId = arguments?.getString(ARG_POST_ID) ?: return@launch
                        PostRepository().addComment(postId, comment)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        /**
         * Creates a new instance of the CommentDialogFragment with the specified post ID.
         *
         * @param postId The ID of the post to which the comment will be added.
         * @return A new instance of CommentDialogFragment with the post ID as an argument.
         */

        fun newInstance(postId: String) = CommentDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_POST_ID, postId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}