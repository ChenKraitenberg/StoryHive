package com.example.storyhive.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storyhive.data.models.User
import com.example.storyhive.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.userProfileChangeRequest

<<<<<<< HEAD
/**
 * ViewModel for handling user authentication.
 * - Supports user sign-up, sign-in, and sign-out.
 * - Manages authentication state via `AuthState` LiveData.
 * - Allows updating user profile with a display name and profile image.
 */
=======
>>>>>>> main
class AuthViewModel : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val authState: LiveData<AuthState> = _authState

<<<<<<< HEAD

    /**
     * Handles user sign-up with email, password, and display name.
     * - Updates the user's display name after sign-up.
     * - Saves user data to Firestore.
     */
=======
>>>>>>> main
    fun signUp(email: String, password: String, displayName: String) {
        _authState.postValue(AuthState.Loading)

        FirebaseRepository.signUp(email, password) { success, error ->
            if (success) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build())
                    ?.addOnCompleteListener { profileUpdate ->
                        if (profileUpdate.isSuccessful) {
                            val newUser = User(uid = user.uid, displayName = displayName)
                            FirebaseRepository.saveUserData(newUser) { isSaved ->
                                if (isSaved) {
                                    _authState.postValue(AuthState.Success)
                                } else {
                                    _authState.postValue(AuthState.Error("Failed to save user data"))
                                }
                            }
                        } else {
                            _authState.postValue(AuthState.Error("Failed to update profile"))
                        }
                    }
            } else {
                _authState.postValue(AuthState.Error(error ?: "Unknown error"))
            }
        }
    }


<<<<<<< HEAD
    /**
     * Handles user sign-in using email and password.
     * - Updates authentication state based on success or failure.
     */
    fun signIn(email: String, password: String) {
=======
    fun signIn(email: String, password: String) {
        // מצב טעינה
>>>>>>> main
        _authState.value = AuthState.Loading

        FirebaseRepository.signIn(email, password) { success, error ->
            if (success) {
<<<<<<< HEAD
                // success connect
                _authState.value = AuthState.Success
            } else {
                // failed connect
=======
                // התחברות מוצלחת
                _authState.value = AuthState.Success
            } else {
                // התחברות נכשלה
>>>>>>> main
                _authState.value = AuthState.Error(error ?: "Unknown error")
            }
        }
    }

<<<<<<< HEAD
    /**
     * Signs out the current user and resets authentication state.
     */
=======
>>>>>>> main
    fun signOut() {
        FirebaseRepository.signOut()
        _authState.value = AuthState.Unauthenticated
    }

<<<<<<< HEAD

    /**
     * Retrieves the currently authenticated user's information.
     * - Returns a `User` object with basic profile details.
     * - Returns `null` if no user is logged in.
     */
    fun getCurrentUser(): User? {
=======
    fun getCurrentUser(): User? {

>>>>>>> main
        return FirebaseRepository.getCurrentUser()?.let { user ->
            user.displayName?.let {
                user.email?.let { it1 ->
                    User(
                        uid = user.uid,
                        displayName = it,
                        email = it1,
                        photoUrl = user.photoUrl.toString()
                    )
                }
            }
        }
<<<<<<< HEAD
    }

    /**
     * Signs up a user with a profile image.
     * - Creates an account and updates the profile with a display name and profile image.
     */
=======
}
>>>>>>> main
    fun signUpWithProfileImage(email: String, password: String, displayName: String, imageUrl: String?) {
        _authState.value = AuthState.Loading

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser

                    if (user != null && imageUrl != null) {
<<<<<<< HEAD
=======
                        // עדכון פרופיל עם שם ותמונה
>>>>>>> main
                        val profileUpdates = userProfileChangeRequest {
                            this.displayName = displayName
                            photoUri = Uri.parse(imageUrl)
                        }

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    _authState.value = AuthState.Success
                                } else {
                                    _authState.value = AuthState.Error(profileTask.exception?.message ?: "Failed to update profile")
                                }
                            }
                    } else {
                        _authState.value = AuthState.Success
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Sign up failed")
                }
            }
<<<<<<< HEAD

=======
>>>>>>> main
    }

}
