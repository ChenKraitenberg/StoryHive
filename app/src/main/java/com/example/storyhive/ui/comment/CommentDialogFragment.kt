package com.example.storyhive.ui.comment

import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.storyhive.databinding.DialogCommentBinding
import com.example.storyhive.repository.PostRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * A dialog fragment that allows users to add a comment to a post.
 * The comment is submitted asynchronously to Firestore via PostRepository.
 */
class CommentDialogFragment : DialogFragment() {
    private var _binding: DialogCommentBinding? = null
    private val binding get() = _binding!!
    private val repository = PostRepository()
    private var commentJob: Job? = null

    // Coroutine exception handler to catch and handle errors gracefully
    private val handler = CoroutineExceptionHandler { _, exception ->
        Log.e("CommentDialog", "Error adding comment", exception)

        // Ensure UI updates run on the main thread
        activity?.runOnUiThread {
            _binding?.let {
                it.commentInput?.isEnabled = true
            }

            // Safely find and enable the positive button (Post button)
            (dialog as? AlertDialog)?.let { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
            }

            val errorMessage = when (exception) {
                is TimeoutCancellationException -> "Operation timed out. Please try again."
                else -> "Failed to add comment: ${exception.localizedMessage}"
            }

            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }


    // Callback to notify when a comment is successfully added
    private var onCommentAdded: (() -> Unit)? = null


    /**
     * Sets a listener to be triggered when a comment is successfully added.
     *
     * @param listener A lambda function to execute after the comment is added.
     */
    fun setOnCommentAddedListener(listener: () -> Unit) {
        onCommentAdded = listener
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        try {
            _binding = DialogCommentBinding.inflate(layoutInflater)

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Comment")
                .setView(binding.root)
                .setPositiveButton("Post", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    if (commentJob?.isActive == true) {
                        Toast.makeText(context, "Please wait, comment is being submitted", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (!isNetworkAvailable()) {
                        Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val comment = binding.commentInput.text.toString().trim()

                    when {
                        comment.isEmpty() -> {
                            Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        comment.length > 300 -> {
                            Toast.makeText(context, "Comment is too long. Maximum 300 characters.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    }

                    // Execute comment submission
                    commentJob = lifecycleScope.launch(Dispatchers.IO + handler) {
                        try {
                            // Ensure the fragment is still active
                            if (!isAdded || isDetached) return@launch

                            val postId = arguments?.getString(ARG_POST_ID)
                                ?: throw Exception("Post ID is missing")

                            // Add a 10-second timeout for the operation
                            withTimeout(10000) {
                                repository.addComment(postId, comment)
                            }

                            // Switch back to the main thread for UI updates
                            withContext(Dispatchers.Main) {
                                // Success message
                                Toast.makeText(context, "Comment added successfully", Toast.LENGTH_SHORT).show()

                                // Clear input field
                                binding.commentInput.text?.clear()

                                // Trigger callback if it exists
                                onCommentAdded?.invoke()

                                // Close dialog
                                dismiss()
                            }
                        } catch (e: Exception) {
                            // Errors will be handled through the coroutine exception handler
                            throw e
                        }
                    }
                }
            }

            return dialog
        } catch (e: Exception) {
            Log.e("CommentDialog", "Error creating dialog", e)
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage("Failed to create comment dialog")
                .setPositiveButton("Close", null)
                .create()
        }
    }

    // check internet connection
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager
        return connectivityManager?.activeNetworkInfo?.isConnected == true
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
        commentJob?.cancel()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        commentJob?.cancel()
        onCommentAdded = null
    }
}