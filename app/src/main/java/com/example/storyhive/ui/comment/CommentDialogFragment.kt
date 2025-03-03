package com.example.storyhive.ui.comment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.storyhive.databinding.DialogCommentBinding
import com.example.storyhive.repository.PostRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler

class CommentDialogFragment : DialogFragment() {

    private var _binding: DialogCommentBinding? = null
    private val binding get() = _binding!!
    private val repository = PostRepository()


    private val handler = CoroutineExceptionHandler { _, exception ->
        Log.e("CommentDialog", "CoroutineExceptionHandler got $exception")
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Failed to add comment: ${exception.message}", Toast.LENGTH_SHORT).show()
            binding.commentInput?.isEnabled = true
        }
    }

    // Callback for when a comment is added
    private var onCommentAdded: (() -> Unit)? = null

    fun setOnCommentAddedListener(listener: () -> Unit) {
        onCommentAdded = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCommentBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Comment")
            .setView(binding.root)
            .setPositiveButton("Post", null)  // שים לב: הגדרנו כאן כNULL
            .setNegativeButton("Cancel", null)
            .create()

        // הגדרת ה-listener ל-show כדי שנוכל להגדיר מחדש את כפתור הPost
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val comment = binding.commentInput.text.toString().trim()
                if (comment.isNotBlank()) {
                    // Execute the comment addition in a coroutine
                    lifecycleScope.launch {
                        try {
                            val postId = arguments?.getString(ARG_POST_ID)
                                ?: throw Exception("Post ID is missing")

                            binding.commentInput.isEnabled = false  // מונע לחיצות נוספות
                            positiveButton.isEnabled = false        // מונע לחיצות נוספות

                            repository.addComment(postId, comment)

                            // Show success toast and notify listener
                            Toast.makeText(requireContext(), "Comment added successfully", Toast.LENGTH_SHORT).show()
                            onCommentAdded?.invoke()
                            dismiss()  // סגור את הדיאלוג רק אחרי הצלחה
                        } catch (e: Exception) {
                            Log.e("CommentDialog", "Error adding comment: ${e.message}", e)
                            Toast.makeText(requireContext(), "Failed to add comment: ${e.message}", Toast.LENGTH_SHORT).show()
                            binding.commentInput.isEnabled = true
                            positiveButton.isEnabled = true
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return dialog
    }

    companion object {
        const val ARG_POST_ID = "post_id"

        fun newInstance(postId: String): CommentDialogFragment {
            val fragment = CommentDialogFragment()
            val args = Bundle().apply {
                putString(ARG_POST_ID, postId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}