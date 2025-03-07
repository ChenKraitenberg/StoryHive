package com.example.storyhive.ui.profile

import StorageRepository
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.storyhive.R
import com.example.storyhive.databinding.DialogEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder

<<<<<<< HEAD
/**
 * DialogFragment for editing user profile details such as name, bio, and profile picture.
 */
=======
>>>>>>> main
class EditProfileDialog : DialogFragment() {
    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private val storageRepository = StorageRepository()
    private var selectedImageUri: Uri? = null

<<<<<<< HEAD
    // Open gallery to select a profile picture
=======
    // פתיחת גלריה לבחירת תמונה
>>>>>>> main
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profileImageView.setImageURI(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCurrentUserData()
        setupClickListeners()
    }

<<<<<<< HEAD
    /**
     * Loads the current user data from Firebase Auth and Firestore.
     */
=======
    // טוען את נתוני המשתמש הנוכחי
// In EditProfileDialog.kt, update the setupCurrentUserData method
>>>>>>> main
    private fun setupCurrentUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            binding.displayNameEditText.setText(user.displayName)

<<<<<<< HEAD
            // Retrieve user bio from Firestore
=======
            // Get user data from Firestore to fill bio field properly
>>>>>>> main
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val bio = document.getString("bio")
<<<<<<< HEAD
                    // Ensure the bio field is properly set
=======
                    // Only set bio text if it's not null or "null" string
>>>>>>> main
                    if (!bio.isNullOrEmpty() && bio != "null") {
                        binding.bioEditText.setText(bio)
                    } else {
                        binding.bioEditText.setText("")  // Empty string, not "null"
                    }
                }

<<<<<<< HEAD
            // Load profile image using Picasso
=======
            // Load profile image
>>>>>>> main
            Picasso.get()
                .load(user.photoUrl)
                .placeholder(R.drawable.baseline_image_24)
                .into(binding.profileImageView)
        }
    }

<<<<<<< HEAD
    /**
     * Sets up click listeners for various UI elements.
     */
=======
    // מאזינים לכפתורים
>>>>>>> main
    private fun setupClickListeners() {
        binding.profileImageView.setOnClickListener { getContent.launch("image/*") }
        binding.changePhotoButton.setOnClickListener { getContent.launch("image/*") }
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.saveButton.setOnClickListener { updateProfile() }
    }

<<<<<<< HEAD
    /**
     * Updates the user profile with the new details entered.
     */
=======
    // עדכון הפרופיל
// Update the updateProfile method in EditProfileDialog.kt
>>>>>>> main
    private fun updateProfile() {
        val newName = binding.displayNameEditText.text.toString()
        val newBio = binding.bioEditText.text.toString() // Ensure this isn't "null" text
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val userUpdates = HashMap<String, Any>()

                // Only add bio if it's not empty
                if (newBio.isNotEmpty() && newBio != "null") {
                    userUpdates["bio"] = newBio
                }

                // Update username
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()

                FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)?.await()

                // Handle profile image
                selectedImageUri?.let { uri ->
                    try {
                        // Upload to Firestore as Base64
                        val imageBase64 = storageRepository.uploadImage(requireContext(), uri, "profile_images")

                        // Store Base64 image in Firestore
                        userUpdates["profileImageBase64"] = imageBase64

<<<<<<< HEAD
                        // Use a placeholder URL for Firebase Auth profile photo
=======
                        // DO NOT try to set this as the photoUri in Firebase Auth
                        // Instead, use a placeholder or actual URL for Auth profile photo
>>>>>>> main
                        val placeholderUrl = "https://ui-avatars.com/api/?name=" +
                                URLEncoder.encode(newName, "UTF-8") + "&background=random&size=100"

                        FirebaseAuth.getInstance().currentUser?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setPhotoUri(Uri.parse(placeholderUrl))
                                .build()
                        )?.await()
                    } catch (e: Exception) {
                        Log.e("EditProfileDialog", "Failed to upload image", e)
                    }
                }

<<<<<<< HEAD
                // Update Firestore with new data
=======
                // Update Firestore
>>>>>>> main
                if (userUpdates.isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update(userUpdates)
                        .await()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    dismiss()
                    (parentFragment as? ProfileFragment)?.refreshProfile()
                }

            } catch (e: Exception) {
                Log.e("EditProfileDialog", "Error updating profile", e)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> main
