// SignUpFragment.kt
package com.example.storyhive.ui.auth

import StorageRepository
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.storyhive.R
import com.example.storyhive.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Sign-up screen for new user registration.
 * - Allows users to register with email, password, and profile image.
 * - Provides image selection from the gallery.
 * - Observes authentication state and navigates to login upon success.
 */
class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private val storageRepository = StorageRepository()

    // Image selection from the gallery
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                selectedImageUri = uri
                updateImagePreview(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }


    /**
     * Called after the view is created.
     * - Sets up event listeners for image selection and registration.
     * - Observes authentication state changes.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImageSelection()
        setupListeners()
        observeViewModel()
    }

    /**
     * Handles user interaction with the profile image container.
     * - Opens an image selection dialog.
     * - Displays an add-photo icon on hover (for desktop use).
     */
    private fun setupImageSelection() {
        binding.profileImageContainer.setOnClickListener {
            showImageOptionsDialog()
        }

        // Show add-photo icon on hover (for desktop usage)
        binding.profileImageContainer.setOnHoverListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    binding.addPhotoIcon.visibility = View.VISIBLE
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    binding.addPhotoIcon.visibility = View.GONE
                }
            }
            true
        }
        // Navigate to login screen
        binding.loginTextView.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }
    }

    /**
     * Displays a dialog for selecting a profile picture.
     * - Allows the user to choose an image from the gallery.
     * - Provides an option to remove the selected image.
     */
    private fun showImageOptionsDialog() {
        val options = arrayOf("Choose from Gallery", "Remove Photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> removeProfileImage()
                }
            }
            .show()
    }


    /**
     * Handles user interactions for signing up, including:
     * - Selecting and updating a profile picture.
     * - Removing a profile picture if needed.
     * - Validating user input and initiating the sign-up process.
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    /**
     * Removes the selected profile image and resets the view to a placeholder.
     */
    private fun removeProfileImage() {
        selectedImageUri = null
        binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
        binding.profileImage.setPadding(
            dpToPx(16),
            dpToPx(16),
            dpToPx(16),
            dpToPx(16)
        )
    }

    /**
     * Updates the profile image preview when a new image is selected.
     */
    private fun updateImagePreview(uri: Uri) {
        binding.profileImage.setImageURI(uri)
        binding.profileImage.setPadding(0, 0, 0, 0) // הסרת הפדינג כשיש תמונה אמיתית
    }

    /**
     * Converts DP (density-independent pixels) to PX (pixels) for UI adjustments.
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }


    /**
     * Sets up event listeners for UI elements, including:
     * - Handling the sign-up button click event.
     * - Validating user input before proceeding.
     * - Uploading an image before completing the sign-up if an image is selected.
     */
    private fun setupListeners() {
        binding.signUpButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (validateInput(name, email, password)) {
                binding.progressBar.visibility = View.VISIBLE
                binding.signUpButton.isEnabled = false

                if (selectedImageUri != null) {
                    // If an image is selected, upload it before signing up
                    uploadImageAndSignUp(name, email, password)
                } else {
                    // Proceed with regular sign-up if no image is selected
                    viewModel.signUp(email, password, name)
                }
            }
        }
    }


    /**
     * Handles user registration along with profile image upload.
     * - Converts the selected image to Base64 format before uploading.
     * - Registers the user with Firebase Authentication.
     * - Stores user details (including Base64 profile image) in Firestore.
     * - Updates Firebase Authentication profile.
     * - Navigates to the home screen upon successful registration.
     */
    private fun uploadImageAndSignUp(name: String, email: String, password: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.signUpButton.isEnabled = false

                Log.d("SignUpFragment", "Starting registration process")

                // Convert image to Base64 format
                var imageBase64: String? = null
                if (selectedImageUri != null) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(
                                requireContext().contentResolver,
                                selectedImageUri
                            )
                        } else {
                            val source = ImageDecoder.createSource(requireContext().contentResolver, selectedImageUri!!)
                            ImageDecoder.decodeBitmap(source)
                        }

                        // Resize the image before converting to Base64
                        val resizedBitmap = storageRepository.getResizedBitmap(bitmap, 500)
                        imageBase64 = storageRepository.encodeImageToBase64(resizedBitmap)

                        Log.d("SignUpFragment", "Image encoded to Base64 successfully")
                    } catch (e: Exception) {
                        Log.e("SignUpFragment", "Failed to encode image", e)
                    }
                }

                // Register user with Firebase Authentication
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        Log.d("SignUpFragment", "User created: ${user?.uid}")

                        if (user != null) {
                            // Save user information in Firestore, including profile image in Base64
                            val userMap = hashMapOf(
                                "userId" to user.uid,
                                "displayName" to name,
                                "profileImageBase64" to (imageBase64 ?: "")
                            )

                            FirebaseFirestore.getInstance().collection("users")
                                .document(user.uid)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Log.d("SignUpFragment", "User saved to Firestore")

                                    // Update user profile in Firebase Authentication
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build()

                                    user.updateProfile(profileUpdates)
                                        .addOnSuccessListener {
                                            Log.d("SignUpFragment", "Auth profile updated successfully")

                                            // Navigate to home screen
                                            binding.progressBar.visibility = View.GONE
                                            Toast.makeText(requireContext(), "ברוך הבא!", Toast.LENGTH_SHORT).show()
                                            findNavController().navigate(R.id.action_signUp_to_home)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("SignUpFragment", "Failed to update auth profile", e)
                                            binding.progressBar.visibility = View.GONE
                                            binding.signUpButton.isEnabled = true
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("SignUpFragment", "Failed to save user to Firestore", e)
                                    binding.progressBar.visibility = View.GONE
                                    binding.signUpButton.isEnabled = true
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("SignUpFragment", "User creation failed", e)
                        binding.progressBar.visibility = View.GONE
                        binding.signUpButton.isEnabled = true
                        Toast.makeText(requireContext(), "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("SignUpFragment", "General error", e)
                binding.progressBar.visibility = View.GONE
                binding.signUpButton.isEnabled = true
            }
        }
    }


    /**
     * Saves user data to Firestore with success/failure handling.
     * - Stores user ID, display name, and profile image URL.
     * - Calls the callback function with `true` if successful, `false` otherwise.
     */
    private fun saveUserToFirestore(userId: String, displayName: String, profileImageUrl: String?, callback: (Boolean) -> Unit) {
        val userMap = hashMapOf(
            "userId" to userId,
            "displayName" to displayName,
            "profileImageUrl" to (profileImageUrl ?: "")
        )

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("SignUpFragment", "User saved to Firestore")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("SignUpFragment", "Failed to save user to Firestore", e)
                callback(false)
            }
    }


    /**
     * Observes authentication state changes and updates the UI accordingly.
     * - Handles successful sign-up and updates Firestore.
     * - Shows errors if sign-up fails.
     * - Navigates to the home screen upon success.
     */
    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Success -> {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        val name = binding.nameInput.text.toString().trim()

                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("SignUpFragment", "User profile updated.")
                                    saveUserToFirestore(user.uid, name, null) { firestoreSuccess ->
                                        if (firestoreSuccess) {
                                            Log.d("SignUpFragment", "User saved to Firestore")
                                        } else {
                                            Log.e("SignUpFragment", "Failed to save user to Firestore")
                                        }
                                    }

                                } else {
                                    Log.e("SignUpFragment", "Failed to update profile", task.exception)
                                }

                                Toast.makeText(requireContext(), "ברוך הבא!", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.action_signUp_to_home)
                            }
                    }
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.signUpButton.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.signUpButton.isEnabled = false
                }
                is AuthState.Unauthenticated -> {
                    binding.progressBar.visibility = View.GONE
                    binding.signUpButton.isEnabled = true
                }
            }
        }
    }

    /**
     * Validates user input for sign-up form.
     * - Ensures name, email, and password fields are filled.
     * - Password must be at least 6 characters.
     * @return `true` if all inputs are valid, `false` otherwise.
     */
    private fun validateInput(name: String, email: String, password: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.nameInput.error = "Name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    /**
     * Cleans up resources when the view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}