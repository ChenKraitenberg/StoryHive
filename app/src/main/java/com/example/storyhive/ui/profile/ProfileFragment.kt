// File: ProfileFragment.kt
package com.example.storyhive.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storyhive.R
import com.example.storyhive.data.models.UserPostsStats
import com.example.storyhive.databinding.FragmentProfileBinding
import com.example.storyhive.ui.home.PostsAdapter
import com.example.storyhive.repository.PostRepository
import com.example.storyhive.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val postsAdapter = PostsAdapter()
    private val postRepository = PostRepository()

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
    }

    private fun setupRecyclerView() {
        binding.postsRecyclerView.apply {
            adapter = postsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

//    private fun setupProfile() {
//        val currentUser = FirebaseAuth.getInstance().currentUser
//
//        currentUser?.let { user ->
//            Log.d("ProfileFragment", "Setting up profile for: ${user.uid}")
//            Log.d("ProfileFragment", "Display name: ${user.displayName}")
//            Log.d("ProfileFragment", "Photo URL: ${user.photoUrl}")
//
//            // עדכון שם המשתמש
//            binding.userName.text = user.displayName ?: "Unknown"
//
//            // תמיד לנסות קודם לטעון מהפיירסטור
//            FirebaseFirestore.getInstance().collection("users")
//                .document(user.uid)
//                .get()
//                .addOnSuccessListener { document ->
//                    if (document.exists()) {
//                        val profileImageUrl = document.getString("profileImageUrl")
//                        Log.d("ProfileFragment", "Firestore profile image URL: $profileImageUrl")
//
//                        if (!profileImageUrl.isNullOrEmpty()) {
//                            // טעינת התמונה מהפיירסטור
//                            Picasso.get()
//                                .load(profileImageUrl)
//                                .placeholder(R.drawable.ic_user_placeholder)
//                                .into(binding.profileImage)
//                        } else if (user.photoUrl != null) {
//                            // אם אין בפיירסטור, ננסה מה-Auth
//                            Log.d("ProfileFragment", "Using Auth photo URL")
//                            Picasso.get()
//                                .load(user.photoUrl)
//                                .placeholder(R.drawable.ic_user_placeholder)
//                                .into(binding.profileImage)
//                        } else {
//                            // אם אין תמונה בכלל, נשתמש בברירת מחדל
//                            binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
//                        }
//                    } else {
//                        // אם המסמך לא קיים, בדוק ב-Auth
//                        if (user.photoUrl != null) {
//                            Picasso.get()
//                                .load(user.photoUrl)
//                                .placeholder(R.drawable.ic_user_placeholder)
//                                .into(binding.profileImage)
//                        } else {
//                            binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
//                        }
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Log.e("ProfileFragment", "Error loading user data", e)
//                    // אם נכשל בטעינה מפיירסטור, ננסה מה-Auth
//                    if (user.photoUrl != null) {
//                        Picasso.get()
//                            .load(user.photoUrl)
//                            .placeholder(R.drawable.ic_user_placeholder)
//                            .into(binding.profileImage)
//                    }
//                }
//
//            // טעינת סטטיסטיקות
//            loadUserStatistics(user.uid)
//        }
//    }
private fun setupProfile() {
    val currentUser = FirebaseAuth.getInstance().currentUser

    currentUser?.let { user ->
        binding.userName.text = user.displayName ?: "Unknown"

        // נסה לטעון תמונת פרופיל מפיירסטור
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profileImageBase64 = document.getString("profileImageBase64")

                    if (!profileImageBase64.isNullOrEmpty()) {
                        try {
                            // המר את ה-base64 לתמונה והצג אותה
                            val bitmap = StorageRepository.decodeBase64ToBitmap(profileImageBase64)
                            binding.profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("ProfileFragment", "Failed to decode profile image", e)
                            binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    } else {
                        binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
                    }
                }
            }
            .addOnFailureListener {
                binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
            }
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
        binding.postsCount.text = "${stats.postsCount} Posts"
        binding.likesCount.text = "${stats.totalLikes} Likes"
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
        binding.emptyStateText.visibility = View.VISIBLE
    }


    fun refreshProfile() {
        setupProfile()
    }

    override fun onResume() {
        super.onResume()
        setupProfile()
    }
}