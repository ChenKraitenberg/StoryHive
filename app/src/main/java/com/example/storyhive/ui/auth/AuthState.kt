package com.example.storyhive.ui.auth

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
