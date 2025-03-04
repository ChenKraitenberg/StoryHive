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

class CommentDialogFragment : DialogFragment() {

    private var _binding: DialogCommentBinding? = null
    private val binding get() = _binding!!
    private val repository = PostRepository()
    private var commentJob: Job? = null

    // מטפל בשגיאות קורוטינה
    private val handler = CoroutineExceptionHandler { _, exception ->
        Log.e("CommentDialog", "Error adding comment", exception)

        // וודא שאתה על Main thread
        activity?.runOnUiThread {
            _binding?.let {
                it.commentInput?.isEnabled = true
            }

            // מצא את הכפתור בצורה בטוחה
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

    // Callback for when a comment is added
    private var onCommentAdded: (() -> Unit)? = null

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
                // מצא את הכפתור בצורה בטוחה
                val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    // בדוק אם יש עבודה פעילה
                    if (commentJob?.isActive == true) {
                        Toast.makeText(context, "Please wait, comment is being submitted", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // בדוק חיבור לאינטרנט
                    if (!isNetworkAvailable()) {
                        Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val comment = binding.commentInput.text.toString().trim()

                    // בדיקות תקינות תוכן
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

                    // הפעל הוספת תגובה
                    commentJob = lifecycleScope.launch(Dispatchers.IO + handler) {
                        try {
                            // וודא שהפרגמנט עדיין פעיל
                            if (!isAdded || isDetached) return@launch

                            val postId = arguments?.getString(ARG_POST_ID)
                                ?: throw Exception("Post ID is missing")

                            // הוסף טיימאווט של 10 שניות
                            withTimeout(10000) {
                                repository.addComment(postId, comment)
                            }

                            // חזור ל-Main thread להודעות ועדכונים
                            withContext(Dispatchers.Main) {
                                // הודעת הצלחה
                                Toast.makeText(context, "Comment added successfully", Toast.LENGTH_SHORT).show()

                                // נקה שדה קלט
                                binding.commentInput.text?.clear()

                                // הפעל callback אם קיים
                                onCommentAdded?.invoke()

                                // סגור דיאלוג
                                dismiss()
                            }
                        } catch (e: Exception) {
                            // השגיאות יטופלו דרך handler
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