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

class AuthViewModel : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val authState: LiveData<AuthState> = _authState

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


    fun signIn(email: String, password: String) {
        // מצב טעינה
        _authState.value = AuthState.Loading

        FirebaseRepository.signIn(email, password) { success, error ->
            if (success) {
                // התחברות מוצלחת
                _authState.value = AuthState.Success
            } else {
                // התחברות נכשלה
                _authState.value = AuthState.Error(error ?: "Unknown error")
            }
        }
    }

    fun signOut() {
        FirebaseRepository.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun getCurrentUser(): User? {

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
}
    fun signUpWithProfileImage(email: String, password: String, displayName: String, imageUrl: String?) {
        _authState.value = AuthState.Loading

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser

                    if (user != null && imageUrl != null) {
                        // עדכון פרופיל עם שם ותמונה
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
    }

}
