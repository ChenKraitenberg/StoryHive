package com.example.storyhive.ui.auth

<<<<<<< HEAD
/**
 * Represents the different states of the authentication process.
 * - `Unauthenticated`: The user is not logged in.
 * - `Loading`: Authentication is in progress.
 * - `Success`: The user has successfully authenticated.
 * - `Error`: Authentication failed, containing an error message.
 */
sealed class AuthState {
    object Unauthenticated : AuthState() // User is not logged in
    object Loading : AuthState() // Authentication is in progress
    object Success : AuthState() // Authentication successful
    data class Error(val message: String) : AuthState() // Authentication failed with an error message
=======
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
>>>>>>> main
}
