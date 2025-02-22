// ui/auth/AuthState.kt
package com.example.storyhive.ui.auth

sealed class AuthState {
    object Loading : AuthState()
    data class Success(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
