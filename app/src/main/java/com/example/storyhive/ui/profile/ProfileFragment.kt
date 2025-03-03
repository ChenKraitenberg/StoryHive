// File: ProfileFragment.kt
package com.example.storyhive.ui.profile

import StorageRepository
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storyhive.R
import com.example.storyhive.data.models.UserPostsStats
import com.example.storyhive.databinding.FragmentProfileBinding
import com.example.storyhive.ui.home.PostsAdapter
import com.example.storyhive.repository.PostRepository
import com.example.storyhive.ui.comment.CommentDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var postsAdapter = PostsAdapter()
    private val postRepository = PostRepository()
    private val viewModel: ProfileViewModel by viewModels() // ✅ הוספת ViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // שחרור ה-Binding כדי למנוע זליגת זיכרון
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProfile()
        setupRecyclerView()
        setupButtons()
        loadUserData()
        setupObservers() // ✅ הוספת מאזין לתוצאה של מחיקה
    }


    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter().apply {
            setOnDeleteClickListener { post ->  // ✅ ודא שהמאזין מחובר
                viewModel.deletePost(post.postId) // ✅ הפעלת המחיקה דרך ה-ViewModel
            }

        // הוספת מאזין ללחיצה על כפתור תגובות
        setOnCommentClickListener { post ->
            CommentDialogFragment.newInstance(post.postId)
                .show(childFragmentManager, "comment_dialog")
        }

        // Add edit click listener
        setOnEditClickListener { post ->
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileToEditPost(post)
            )
        }
    }

    binding.postsRecyclerView.apply {
        adapter = postsAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
}

    private fun setupObservers() {
        viewModel.deleteStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                loadUserData() // ✅ ריענון הפוסטים אחרי מחיקה מוצלחת
            } else {
                Log.e("ProfileFragment", "שגיאה במחיקת הפוסט")
            }
        }
    }


    private fun setupProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            binding.usernameTextView.text = user.displayName ?: "Unknown"

            // נסה לטעון תמונת פרופיל ונתונים נוספים מפיירסטור
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // טען את תמונת הפרופיל
                        val profileImageBase64 = document.getString("profileImageBase64")

                        // טען את הביו
                        val bio = document.getString("bio")
                        binding.bioTextView.text = bio ?: ""

                        if (!profileImageBase64.isNullOrEmpty()) {
                            try {
                                // המר את ה-base64 לתמונה והצג אותה
                                val bitmap = StorageRepository().decodeBase64ToBitmap(profileImageBase64)
                                binding.profileImageView.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                Log.e("ProfileFragment", "Failed to decode profile image", e)
                                binding.profileImageView.setImageResource(R.drawable.ic_user_placeholder)
                            }
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    }
                }
                .addOnFailureListener {
                    binding.profileImageView.setImageResource(R.drawable.ic_user_placeholder)
                }

            loadUserStatistics(user.uid) // טעינת סטטיסטיקות
        }
    }
    private fun loadUserStatistics(userId: String) {
        val db = FirebaseFirestore.getInstance()

        // ספירת פוסטים ולייקים
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val postsCount = querySnapshot.size()

                // חישוב מספר לייקים כולל
                var totalLikes = 0
                for (document in querySnapshot.documents) {
                    val likes = document.getLong("likes") ?: 0
                    totalLikes += likes.toInt()
                }

                // יצירת אובייקט הסטטיסטיקות
                val stats = UserPostsStats(postsCount, totalLikes)

                // עדכון התצוגה
                updateStatsUI(stats)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error getting user posts", e)
                // במקרה של שגיאה, הצג סטטיסטיקות אפס
                updateStatsUI(UserPostsStats(0, 0))
            }
    }

    private fun updateStatsUI(stats: UserPostsStats) {
        binding.postsCountTextView.text = "${stats.postsCount} Posts"
        binding.likesCountTextView.text = "${stats.totalLikes} Likes"
    }
    private fun setupButtons() {
        binding.editProfileButton.setOnClickListener {
            // Open edit profile dialog
            EditProfileDialog().show(childFragmentManager, "edit_profile")
        }

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun loadUserData() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            postRepository.observeUserPosts(userId) { posts ->
                postsAdapter.submitList(posts)
                updateEmptyState(posts.isEmpty())
            }
        }
    }

    private fun updateEmptyState(empty: Boolean) {
        val binding = _binding ?: return // אם binding null, צאי מהפונקציה

        if (empty) {
            // אין פוסטים - הצג את הודעת "No posts yet"
            binding.noPostsTextView.visibility = View.VISIBLE
            binding.postsRecyclerView.visibility = View.GONE
        } else {
            // יש פוסטים - הסתר את ההודעה והצג את הרשימה
            binding.noPostsTextView.visibility = View.GONE
            binding.postsRecyclerView.visibility = View.VISIBLE
        }
    }



    fun refreshProfile() {
        setupProfile()
    }

    override fun onResume() {
        super.onResume()
        setupProfile()
    }
}