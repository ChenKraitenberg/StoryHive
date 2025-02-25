package com.example.storyhive.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleBooksApiService {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("key") apiKey: String = "AIzaSyCacwYk8Llj3GoA6rwi8PMqh5r3X6gNl6Y",
        @Query("maxResults") maxResults: Int = 10
    ): GoogleBooksResponse

    @GET("volumes/{bookId}")
    suspend fun getBookDetails(
        @Path("bookId") bookId: String
    ): GoogleBookItem
}

data class GoogleBooksResponse(
    val items: List<GoogleBookItem>
)

data class GoogleBookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String,
    val authors: List<String>? = null,
    val description: String? = null,
    val imageLinks: ImageLinks? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val previewLink: String? = null
)

data class ImageLinks(
    val thumbnail: String? = null,
    val smallThumbnail: String? = null
)

object RetrofitClient {
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    val googleBooksService: GoogleBooksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApiService::class.java)
    }
}