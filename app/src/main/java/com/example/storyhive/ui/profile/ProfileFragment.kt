// File: ProfileFragment.kt
package com.example.storyhive.ui.profile

import StorageRepository
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storyhive.R
import com.example.storyhive.StoryHiveApplication
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.local.StoryHiveDatabase
import com.example.storyhive.data.models.UserPostsStats
import com.example.storyhive.databinding.FragmentProfileBinding
import com.example.storyhive.ui.home.PostsAdapter
import com.example.storyhive.repository.PostRepository
import com.example.storyhive.ui.comment.CommentDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Fragment responsible for displaying the user's profile, including their posts and profile details.
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var postsAdapter: PostsAdapter
    private val postRepository = PostRepository()
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var imageCacheManager: ImageCacheManager
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Release the binding to prevent memory leaks
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initImageCacheManager()
        setupProfile()
        setupRecyclerView()
        setupButtons()
        loadUserData()
        setupObservers() // Added observer for delete operation result
    }

    /**
     * Initializes the ImageCacheManager, either retrieving it from the application instance
     * or creating a new one if unavailable.
     */
    private fun initImageCacheManager() {
        // Try to get the ImageCacheManager from the application
        imageCacheManager = try {
            (requireActivity().application as StoryHiveApplication).imageCacheManager
        } catch (e: Exception) {
            // If not available, create a new instance
            val database = StoryHiveDatabase.getInstance(requireContext())
            ImageCacheManager.getInstance(requireContext(), database.imageCacheDao())
        }
    }


    /**
     * Sets up the RecyclerView for displaying user posts.
     */
    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter(imageCacheManager).apply {
            //  Ensure the delete listener is properly set
            setOnDeleteClickListener { post ->
                viewModel.deletePost(post.postId) // Trigger delete operation via ViewModel
            }

            // Add listener for comment button click
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


            // Handle comment count click
            setOnCommentCountClickListener { post ->
                try {
                    val action = ProfileFragmentDirections.actionProfileToComments(
                        postId = post.postId,
                        postTitle = post.bookTitle
                    )
                    if (isAdded && !isDetached) {
                        findNavController().navigate(action)
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error navigating to comments: ${e.message}", e)
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
    }


    private fun setupObservers() {
        viewModel.deleteStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                loadUserData() // Refresh posts after successful deletion
            } else {
                Log.e("ProfileFragment", "Error deleting post")
            }
        }
    }

    /**
     * Sets up the user profile by fetching and displaying user details from Firebase Auth and Firestore.
     */
    private fun setupProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            binding.usernameTextView.text = user.displayName ?: "Unknown"

            // Fetch user data from Firestore
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Handle profile image retrieval from Base64
                        val profileImageBase64 = document.getString("profileImageBase64")

                        // Handle bio text properly (avoid displaying "null" as text)
                        val bio = document.getString("bio")
                        if (!bio.isNullOrEmpty() && bio != "null") {
                            binding.bioTextView.text = bio
                            binding.bioTextView.visibility = View.VISIBLE
                        } else {
                            // Hide the bio TextView if there's no valid bio
                            binding.bioTextView.visibility = View.GONE
                        }

                        // Profile image display
                        if (!profileImageBase64.isNullOrEmpty()) {
                            try {
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

            loadUserStatistics(user.uid)
        }
    }


    /**
     * Handles loading and displaying the user's profile image.
     * Tries Base64 first, then cached URL, then network, and falls back to a placeholder if needed.
     */
    private fun processProfileImage(profileImageBase64: String?, photoUrl: String?) {
        // First try to load Base64 image if available
        if (!profileImageBase64.isNullOrEmpty()) {
            try {
                val bitmap = StorageRepository().decodeBase64ToBitmap(profileImageBase64)
                binding.profileImageView.setImageBitmap(bitmap)
                return
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Failed to decode profile image", e)
                // If Base64 fails, continue to the next option
            }
        }

        // If no Base64 image, try loading the profile image from the URL with caching
        if (!photoUrl.isNullOrEmpty()) {
            coroutineScope.launch {
                val localPath = withContext(Dispatchers.IO) {
                    imageCacheManager.getLocalPathForUrl(photoUrl)
                }

                if (localPath != null) {
                    // Load from local cache
                    Picasso.get()
                        .load(File(localPath))
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(binding.profileImageView)
                } else {
                    // Load from network
                    Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(binding.profileImageView)

                    // Cache the image in background
                    coroutineScope.launch(Dispatchers.IO) {
                        imageCacheManager.cacheImage(photoUrl)
                    }
                }
            }
            return
        }

        // Fallback to placeholder if neither base64 nor URL is available
        binding.profileImageView.setImageResource(R.drawable.ic_user_placeholder)
    }

    private fun loadUserStatistics(userId: String) {
        val db = FirebaseFirestore.getInstance()

        // Count user posts and total likes
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val postsCount = querySnapshot.size()
                // Calculate total likes from all user posts
                var totalLikes = 0
                for (document in querySnapshot.documents) {
                    val likes = document.getLong("likes") ?: 0
                    totalLikes += likes.toInt()
                }
                // Create statistics object
                val stats = UserPostsStats(postsCount, totalLikes)
                // Update UI with statistics
                updateStatsUI(stats)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error getting user posts", e)
                // In case of failure, set statistics to zero
                updateStatsUI(UserPostsStats(0, 0))
            }
    }


    /**
     * Updates the UI with the user's post count and total likes.
     */
    private fun updateStatsUI(stats: UserPostsStats) {
        binding.postsCountTextView.text = "${stats.postsCount} Posts"
        binding.likesCountTextView.text = "${stats.totalLikes} Likes"
    }

    /**
     * Sets up click listeners for profile-related actions, such as editing the profile and logging out.
     */
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

    /**
     * Loads the current user's posts from Firestore and updates the UI accordingly.
     */
    private fun loadUserData() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            postRepository.observeUserPosts(userId) { posts ->
                postsAdapter.submitList(posts)
                updateEmptyState(posts.isEmpty())
            }
        }
    }


    /**
     * Updates the UI to display or hide the "No posts yet" message based on the post count.
     */
    private fun updateEmptyState(empty: Boolean) {
        val binding = _binding ?: return // Exit if binding is null

        if (empty) {
            // No posts available - show the "No posts yet" message
            binding.noPostsTextView.visibility = View.VISIBLE
            binding.postsRecyclerView.visibility = View.GONE
        } else {
            // Posts available - hide the message and show the list
            binding.noPostsTextView.visibility = View.GONE
            binding.postsRecyclerView.visibility = View.VISIBLE
        }
    }

    /**
     * Refreshes the user profile by reloading user data.
     */
    fun refreshProfile() {
        setupProfile()
    }

    /**
     * Reloads the user profile data every time the fragment resumes.
     */
    override fun onResume() {
        super.onResume()
        setupProfile()
    }
}