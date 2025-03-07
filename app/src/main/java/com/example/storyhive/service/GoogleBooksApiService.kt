package com.example.storyhive.service

import com.example.storyhive.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

//Service interface for accessing the Google Books API using Retrofit
interface GoogleBooksApiService {

    /**
     * Searches for books using the Google Books API.
     * @param query The search keyword(s).
     * @param apiKey The API key for authentication.
     * @param maxResults The maximum number of results to retrieve (default is 10).
     * @param printType The type of content to search for (default is "books").
     * @return A response containing a list of books that match the query.
     */

    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("key") apiKey: String = BuildConfig.GOOGLE_BOOKS_API_KEY,
        @Query("maxResults") maxResults: Int = 10,
        @Query("printType") printType: String = "books"


    ): GoogleBooksResponse


    /**
     * Retrieves detailed information about a specific book.
     * @param bookId The unique ID of the book.
     * @return The book details including title, author, description, and more.
     */

    @GET("volumes/{bookId}")
    suspend fun getBookDetails(
        @Path("bookId") bookId: String
    ): GoogleBookItem
}

//Data class representing the response from a book search query
data class GoogleBooksResponse(
    val items: List<GoogleBookItem>
)

//Data class representing an individual book item returned from the API
data class GoogleBookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

//Data class representing detailed information about a book
data class VolumeInfo(
    val title: String,
    val authors: List<String>? = null,
    val description: String? = null,
    val imageLinks: ImageLinks? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val previewLink: String? = null
)

//Data class representing image links for a book
data class ImageLinks(
    val thumbnail: String? = null,
    val smallThumbnail: String? = null
)

//ingleton object that provides a Retrofit client for accessing the Google Books API
object RetrofitClient {
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    //Lazy-initialized Retrofit instance for Google Books API service
    val googleBooksService: GoogleBooksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApiService::class.java)
    }
}