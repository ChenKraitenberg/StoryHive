package com.example.storyhive.ui.addPost

import com.example.storyhive.StoryHiveApplication
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.local.StoryHiveDatabase
import com.example.storyhive.data.models.Post
import com.example.storyhive.R
import com.example.storyhive.databinding.FragmentCreatePostBinding
import java.io.File
import android.app.Application
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.util.Log
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import StorageRepository


/**
 * Fragment for editing an existing post.
 * - Loads the current post details and allows the user to update them.
 * - Supports updating the book title, author, review, rating, and image.
 * - Handles image selection from gallery or camera.
 * - Uses ViewModel to manage UI state and update the post in Firebase.
 */

class EditPostFragment : Fragment() {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditPostViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private val args: EditPostFragmentArgs by navArgs()
    private lateinit var imageCacheManager: ImageCacheManager

    /**
     * Handles selecting an image from the gallery.
     * When an image is selected, it updates the UI and ViewModel with the new image URI.
     */
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                // Obtain persistent permission for the URI
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedImageUri = uri
                binding.bookImage.setImageURI(uri)
                viewModel.setSelectedImage(uri)
            }
        }
    }



    /**
     * Handles capturing an image using the device's camera.
     * Updates the UI and ViewModel with the captured image.
     */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            selectedImageUri?.let { uri ->
                binding.bookImage.setImageURI(uri)
                viewModel.setSelectedImage(uri)
            }
        }
    }


    /**
     * Handles requesting storage permissions.
     * If granted, it opens the image selection dialog.
     * If denied, it shows a message informing the user about the requirement.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(
                requireContext(),
                "Permission is required to select an image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Inflates the fragment's layout and initializes view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }


    /**
     * Called after the view is created.
     * Initializes the image cache manager, sets up listeners, and populates the UI with the existing post data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initImageCacheManager()
        // Set up the view with existing post data
        setupWithExistingPost(args.post)
        setupListeners()
        observeViewModel()
    }



    /**
     * Initializes the ImageCacheManager by retrieving it from the application instance if available.
     * If not found, it creates a new instance using the local database.
     */
    private fun initImageCacheManager() {
        imageCacheManager = try {
            // Retrieve ImageCacheManager from the application instance if configured
            (requireActivity().application as StoryHiveApplication).imageCacheManager
        } catch (e: Exception) {
            // If StoryHiveApplication is not available, create a new instance
            val database = StoryHiveDatabase.getInstance(requireContext())
            ImageCacheManager.getInstance(requireContext(), database.imageCacheDao())
        }
    }


    /**
     * Populates the UI with existing post data for editing.
     * - Fills the input fields with the post's current details.
     * - Loads and caches the existing post image, if available.
     * - Updates the button text to "Update Post".
     * - Initializes the ViewModel with the existing post.
     */
    private fun setupWithExistingPost(post: Post) {
        // Fill the form with existing post data
        binding.bookTitleInput.setText(post.bookTitle)
        binding.authorInput.setText(post.bookAuthor)
        binding.reviewInput.setText(post.review)

        // First try to load the image URL if it exists
        if (!post.imageUrl.isNullOrEmpty()) {
            lifecycleScope.launch {
                val localPath = withContext(Dispatchers.IO) {
                    imageCacheManager.getLocalPathForUrl(post.imageUrl!!)
                }

                if (localPath != null) {
                    // Load image from local cache
                    Picasso.get()
                        .load(File(localPath))
                        .placeholder(R.drawable.ic_book_placeholder)
                        .into(binding.bookImage)
                } else {
                    // Load image from the network
                    Picasso.get()
                        .load(post.imageUrl)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .into(binding.bookImage)

                    // Cache the image in the background
                    lifecycleScope.launch(Dispatchers.IO) {
                        imageCacheManager.cacheImage(post.imageUrl!!)
                    }
                }
            }
        }
        // If no URL but Base64 image exists, decode and display it
        else if (!post.imageBase64.isNullOrEmpty()) {
            try {
                Log.d("EditPostFragment", "Loading Base64 image")
                val bitmap = StorageRepository().decodeBase64ToBitmap(post.imageBase64!!)
                if (bitmap != null) {
                    binding.bookImage.setImageBitmap(bitmap)
                } else {
                    // Fallback to placeholder if decoding fails
                    binding.bookImage.setImageResource(R.drawable.ic_book_placeholder)
                }
            } catch (e: Exception) {
                Log.e("EditPostFragment", "Failed to decode Base64 image", e)
                binding.bookImage.setImageResource(R.drawable.ic_book_placeholder)
            }
        }
        // If no image at all, just show the placeholder
        else {
            binding.bookImage.setImageResource(R.drawable.ic_book_placeholder)
        }

        // Update button text to indicate updating an existing post
        binding.publishButton.text = "Update Post"

        // Initialize the ViewModel with the existing post data
        viewModel.initWithPost(post)
    }


    /**
     * Sets up click listeners for UI elements:
     * - Opens the image selection dialog when the image card is clicked.
     * - Checks and requests permissions if needed.
     */
    private fun setupListeners() {
        binding.imageCard.setOnClickListener {
            // Check and request permissions based on Android version
            if (hasStoragePermission()) {
                showImageSourceDialog()
            } else {
                requestStoragePermission()
            }
        }

        // Submit post button listener
        binding.publishButton.setOnClickListener {
            val title = binding.bookTitleInput.text.toString()
            val author = binding.authorInput.text.toString()
            val review = binding.reviewInput.text.toString()

            if (validateInput(title, author, review)) {
                viewModel.updatePost(requireContext(), title, author, review)
            }
        }
    }



    /**
     * Checks if the app has permission to access storage for selecting images.
     * Uses different permissions based on Android version.
     * @return True if permission is granted, false otherwise.
     */
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }



    /**
     * Requests storage permission for selecting images.
     * Uses different permissions based on Android version.
     */
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }



    /**
     * Handles the result of permission requests for storage and camera access.
     * If granted, proceeds with image selection or camera capture.
     * If denied, shows a message informing the user about the requirement.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageSourceDialog() // Open image selection dialog
            } else {
                Toast.makeText(
                    requireContext(),
                    "Storage permission is required to select images",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturePhoto() // Open camera to take a photo

            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to take photos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Displays a dialog allowing the user to choose an image source:
     * - "Choose from Gallery" opens the device's image gallery.
     * - "Take Photo" checks for camera permission and captures an image.
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> checkCameraPermission()
                }
            }
            .show()
    }

    /**
     * Checks if the app has camera permission.
     * - If granted, opens the camera to take a photo.
     * - If not, requests camera permission from the user.
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            capturePhoto()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }


    /**
     * Opens the device's image gallery to allow the user to select a picture.
     * Uses an intent to launch the gallery app.
     */
    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"))
        } catch (e: Exception) {
            Log.e("ImagePicker", "Error opening gallery", e)
            Toast.makeText(
                requireContext(),
                "Failed to open gallery",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        // Request codes for storage and camera permissions
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }



    /**
     * Captures a photo using the device's camera.
     * - Creates a temporary file in the cache directory to store the image.
     * - Generates a content URI using FileProvider.
     * - Launches the camera app with the generated URI.
     */
    private fun capturePhoto() {
        val photoFile = File(requireContext().cacheDir, "book_image.jpg")
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        selectedImageUri = photoUri
        cameraLauncher.launch(photoUri)
    }



    /**
     * Validates user input for creating or updating a post.
     * Ensures that the book title, author name, and review fields are not empty.
     * - Displays an error message if a field is blank.
     * - Returns true if all inputs are valid, false otherwise.
     */
    private fun validateInput(title: String, author: String, review: String): Boolean {
        var isValid = true

        // Validate book title
        if (title.isBlank()) {
            binding.bookTitleLayout.error = "Please enter book title"
            isValid = false
        } else {
            binding.bookTitleLayout.error = null
        }

        // Validate author name
        if (author.isBlank()) {
            binding.authorLayout.error = "Please enter author name"
            isValid = false
        } else {
            binding.authorLayout.error = null
        }

        // Validate review content
        if (review.isBlank()) {
            binding.reviewLayout.error = "Please write your review"
            isValid = false
        } else {
            binding.reviewLayout.error = null
        }

        return isValid
    }



    /**
     * Observes changes in the ViewModel's UI state and updates the UI accordingly:
     * - Shows a loading indicator when the post is being updated.
     * - Displays a success message and navigates back when the update is successful.
     * - Shows an error message if the update fails.
     * - Resets UI elements to their initial state when required.
     */
    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditPostUiState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.publishButton.isEnabled = false
                }
                is EditPostUiState.Success -> {
                    Toast.makeText(context, "Post updated successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is EditPostUiState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.publishButton.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                is EditPostUiState.Initial -> {
                    binding.progressBar.isVisible = false
                    binding.publishButton.isEnabled = true
                }
            }
        }
    }



    /**
     * Cleans up view binding when the fragment's view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}