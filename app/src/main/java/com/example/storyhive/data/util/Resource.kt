package com.example.storyhive.data.util

/**
 * A generic class for managing and passing result states: Loading, Success, or Error.
 * Used as a wrapper for data returned from the repository and as a basis for UI decisions.
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * Represents a successful data loading state.
     * @param data The data retrieved from the source.
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Represents an error state when loading data.
     * @param message The error message.
     * @param data Optional data that might still be available despite the error (e.g., from a local cache).
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Represents a state where data is being loaded.
     * @param data Temporary data that might be available during loading (e.g., from a local cache).
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)

    //Checks if the resource is in a loading state
    fun isLoading(): Boolean = this is Loading


    //Checks if the resource is in a success state.
    fun isSuccess(): Boolean = this is Success

    //Checks if the resource is in an error state.
    fun isError(): Boolean = this is Error

    //Checks if the resource contains available data
    fun hasData(): Boolean = data != null

    companion object {
        //Creates a Resource.Loading object with optional data
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)

        //Creates a Resource.Success object with the received data
        fun <T> success(data: T): Resource<T> = Success(data)

        //Creates a Resource.Error object with an error message and optional data
        fun <T> error(message: String, data: T? = null): Resource<T> = Error(message, data)
    }
}