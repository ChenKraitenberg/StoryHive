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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storyhive.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.example.storyhive.StoryHiveApplication
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.local.StoryHiveDatabase
import com.example.storyhive.data.util.Resource
import com.example.storyhive.ui.comment.CommentDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


/**
 * HomeFragment displays the main feed of posts, allowing users to view, like, edit,
 * delete, and comment on posts.
 */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var imageCacheManager: ImageCacheManager

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
        initImageCacheManager()
        setupRecyclerView()
        setupFlowCollectors()
    }


    /**
     * Initializes the ImageCacheManager from the application instance.
     * If the application does not provide one, a new instance is created.
     */
    private fun initImageCacheManager() {
        imageCacheManager = try {
            (requireActivity().application as StoryHiveApplication).imageCacheManager
        } catch (e: Exception) {
            val database = StoryHiveDatabase.getInstance(requireContext())
            ImageCacheManager.getInstance(requireContext(), database.imageCacheDao())
        }
    }


    /**
     * Sets up the RecyclerView with the PostsAdapter and defines interaction listeners
     * for post actions such as liking, editing, deleting, and commenting.
     */
    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter(imageCacheManager).apply {

            // Handle post like action
            setOnLikeClickListener { post ->
                viewModel.likePost(post.postId)
            }


            // Handle post edit action
            setOnEditClickListener { post ->
                // Navigate to the edit post screen with the post as an argument
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToEditPost(post)
                )
            }

            // Handle post delete action
            setOnDeleteClickListener { post ->
                viewModel.deletePost(post.postId)
            }

            // Handle post comment action
            setOnCommentClickListener { post ->
                // Create and show the comment dialog with a callback for refreshing posts
                val dialog = CommentDialogFragment.newInstance(post.postId)
                dialog.setOnCommentAddedListener {
                    // Called when a comment is successfully added
                    viewModel.refreshPosts()
                }
                dialog.show(childFragmentManager, "comment_dialog")
            }

            // Navigate to the comments screen when comment count is clicked
            setOnCommentCountClickListener { post ->
                try {
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
                        Toast.makeText(it, "Error loading comments screen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Initialize RecyclerView
        binding.postsRecyclerView.apply {
            adapter = postsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }
    }


    private fun setupFlowCollectors() {
        // Collect the flow of posts
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.posts.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Show progress bar only if there are no existing posts
                            binding.progressBar.isVisible = postsAdapter.itemCount == 0
                            // If there are posts, show a refresh indicator instead
                            binding.swipeRefresh.isRefreshing = postsAdapter.itemCount > 0

                        }
                        is Resource.Success -> {
                            // Hide progress indicators when data is successfully loaded
                            binding.progressBar.isVisible = false
                            binding.swipeRefresh.isRefreshing = false


                            resource.data?.let { posts ->
                                // Update the RecyclerView with new posts
                                postsAdapter.submitList(posts)
                                updateEmptyState(posts.isEmpty())
                            }
                        }
                        is Resource.Error -> {
                            // Hide progress indicators in case of an error
                            binding.progressBar.isVisible = false
                            binding.swipeRefresh.isRefreshing = false

                            // If there are cached/previously loaded posts, display them despite the error
                            resource.data?.let { posts ->
                                postsAdapter.submitList(posts)
                                updateEmptyState(posts.isEmpty())

                                // Show error message but keep displaying the available data
                                Toast.makeText(requireContext(),
                                    "Error loading latest posts: ${resource.message}",
                                    Toast.LENGTH_LONG).show()
                            } ?: run {
                                // No data at all, display a full error message
                                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        // Collect the flow of post deletion status
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteStatus.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                        }
                        is Resource.Success -> {
                            if (resource.data == true) {
                                Toast.makeText(requireContext(),
                                    "Post deleted successfully!",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(),
                                "Error deleting post: ${resource.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


    /**
     * Updates the UI state when there are no posts available.
     * If `isEmpty` is true, the empty state message is shown; otherwise, it is hidden.
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateText.isVisible = isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}