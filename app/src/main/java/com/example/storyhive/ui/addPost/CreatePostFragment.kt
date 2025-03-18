package com.example.storyhive.ui.addPost

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.storyhive.databinding.FragmentCreatePostBinding
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.storyhive.StoryHiveApplication
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.local.StoryHiveDatabase
import com.example.storyhive.data.models.Post


class CreatePostFragment : Fragment() {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePostViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private lateinit var imageCacheManager: ImageCacheManager

    // Gallery image selection
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


    // Camera image capture
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

    // Permission request launcher
    // Handles permission request for accessing the gallery or camera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImageSourceDialog() // Show dialog to select image source (camera/gallery)
        } else {
            Toast.makeText(
                requireContext(),
                "Permission is required to select an image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //Inflates the fragment's layout and initializes view binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called after the view is created. Initializes image cache, sets up listeners,
     * observes ViewModel changes, and pre-fills book details if available.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initImageCacheManager() // Initialize the image cache manager
        setupListeners() // Set up click listeners for UI elements
        observeViewModel() // Observe changes in the ViewModel

        val bookId = arguments?.getString("bookId") ?: ""
        val bookTitle = arguments?.getString("bookTitle") ?: ""

        // If book info is pre-filled, disable manual input
        if (bookId.isNotEmpty()) {
            binding.bookTitleInput.setText(bookTitle)
            binding.bookTitleInput.isEnabled = false
        }

        // Create a post object including the bookId
        val post = Post(
            // your existing fields
            bookId = bookId,
            // other fields
        )
    }

    /**
     * Initializes the ImageCacheManager by retrieving it from the application instance if available.
     * If not found, it creates a new instance using the local database.
     */
    private fun initImageCacheManager() {
        // Retrieve ImageCacheManager from the application instance if configured
        imageCacheManager = try {
            (requireActivity().application as StoryHiveApplication).imageCacheManager
        } catch (e: Exception) {
            // If StoryHiveApplication is not available, create a new instance
            val database = StoryHiveDatabase.getInstance(requireContext())
            ImageCacheManager.getInstance(requireContext(), database.imageCacheDao())
        }
    }


    /**
     * Sets up click listeners for UI elements:
     * - Opens the image selection dialog when the image card is clicked.
     * - Validates input and submits the post when the publish button is clicked.
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
                viewModel.createPost(requireContext(), title, author, review, 0.0f)
            }
        }
    }


    /**
     * Checks if the app has permission to access storage for selecting images.
     * Returns true if permission is granted, false otherwise.
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
                showImageSourceDialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Storage permission is required to select images",
                    Toast.LENGTH_SHORT
                ).show() // Open camera to take a photo
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturePhoto()
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
     * Displays a dialog for the user to select an image source:
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
     * Validates user input for creating a post.
     * Checks that the book title, author name, and review are not empty.
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

        // Validate author
        if (author.isBlank()) {
            binding.authorLayout.error = "Please enter author name"
            isValid = false
        } else {
            binding.authorLayout.error = null
        }

        // Validate review
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
     * - Shows a loading indicator when a post is being created.
     * - Displays a success message and navigates back when post creation is successful.
     * - Shows an error message if post creation fails.
     * - Resets UI elements to their initial state when required.
     */
    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreatePostUiState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.publishButton.isEnabled = false
                }

                is CreatePostUiState.Success -> {
                    Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp() // Navigate back after successful post creation
                }

                is CreatePostUiState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.publishButton.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }

                is CreatePostUiState.Initial -> {
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