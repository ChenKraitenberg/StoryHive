package com.example.storyhive.ui.profile

import StorageRepository
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
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
import kotlinx.coroutines.launch

class EditProfileDialog : DialogFragment() {
    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private val storageRepository = StorageRepository()
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profileImage.setImageURI(it)
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

    private fun setupCurrentUserData() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            binding.nameInput.setText(user.displayName)

            // טעינת תמונת פרופיל נוכחית
            user.photoUrl?.let { uri ->
                Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(binding.profileImage)
            }
        }
    }

    private fun setupClickListeners() {
        binding.profileImageContainer.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.profileImageContainer.setOnHoverListener { _, event ->
            binding.editImageButton.visibility = if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                View.VISIBLE
            } else {
                View.GONE
            }
            true
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        val newName = binding.nameInput.text.toString()
        val newBio = binding.bioInput.text.toString()

        lifecycleScope.launch {
            try {
                // עדכון תמונת פרופיל אם נבחרה חדשה
                val photoUrl = selectedImageUri?.let { uri ->
                    FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
                        storageRepository.uploadImage(requireContext(), uri, "profile_images/$userId")
                    }
                }

                // עדכון פרטי המשתמש
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .apply {
                        photoUrl?.let { setPhotoUri(Uri.parse(it.toString())) }
                    }
                    .build()

                FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)
                    ?.addOnSuccessListener {
                        // עדכון הביוגרפיה בFirestore
                        if (newBio.isNotEmpty()) {
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(FirebaseAuth.getInstance().currentUser!!.uid)
                                .update("bio", newBio)
                        }

                        dismiss()
                        (parentFragment as? ProfileFragment)?.refreshProfile()
                    }
            } catch (e: Exception) {
                // טיפול בשגיאות
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}