// viewmodels/AuthViewModel.kt
package com.example.storyhive.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storyhive.ui.auth.AuthState

class AuthViewModel : ViewModel() {
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        // בשלב זה נדמה הצלחה
        _authState.postValue(AuthState.Success("dummy_user_id"))
    }

    fun signUp(name: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        // בשלב זה נדמה הצלחה
        _authState.postValue(AuthState.Success("dummy_user_id"))
    }
}