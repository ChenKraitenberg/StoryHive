package com.example.storyhive.ui.addPost

import StorageRepository
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
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.storyhive.R
import com.example.storyhive.databinding.FragmentCreatePostBinding
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.storyhive.data.models.Post

class EditPostFragment : Fragment() {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditPostViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private val args: EditPostFragmentArgs by navArgs()

    // Gallery image selection
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                // קבל הרשאת קבע על URI
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the view with existing post data
        setupWithExistingPost(args.post)

        setupListeners()
        observeViewModel()
    }

    private fun setupWithExistingPost(post: Post) {
        // Fill the form with existing post data
        binding.bookTitleInput.setText(post.bookTitle)
        binding.authorInput.setText(post.bookAuthor)
        binding.reviewInput.setText(post.review)
        binding.ratingBar.rating = post.rating

        // If there's an image URL, load the image
        if (!post.imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(post.imageUrl)
                .into(binding.bookImage)
        } else if (!post.imageBase64.isNullOrEmpty()) {
            // If there's a base64 image, try to decode and display it
            try {
                val bitmap = StorageRepository().decodeBase64ToBitmap(post.imageBase64!!)
                binding.bookImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("EditPostFragment", "Failed to decode image", e)
            }
        }

        // Update button text
        binding.publishButton.text = "Update Post"

        // Initialize the view model with the post
        viewModel.initWithPost(post)
    }

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
            val rating = binding.ratingBar.rating

            if (validateInput(title, author, review)) {
                viewModel.updatePost(requireContext(), title, author, review, rating)
            }
        }
    }

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
                ).show()
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
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}