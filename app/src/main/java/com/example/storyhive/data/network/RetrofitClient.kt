package com.example.storyhive.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.storyhive.service.GoogleBooksApiService
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object RetrofitClient {
    private lateinit var context: Context
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"
    private const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MB

    //Initializes the RetrofitClient with the application context
    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    //Defines the cache with a maximum size
    private val cache by lazy {
        Cache(context.cacheDir, CACHE_SIZE.toLong())
    }

    //Interceptor for caching responses to optimize network usage
    private val cacheInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        val cacheControl = CacheControl.Builder()
            .maxAge(7, TimeUnit.DAYS) // Cache responses for up to 7 days
            .build()

        response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }

    //OkHttpClient with caching and timeout settings
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor(cacheInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                val isConnected = isNetworkAvailable()

                val request = if (isConnected) {
                    // If network is available, fetch fresh data
                    originalRequest.newBuilder()
                        .header("Cache-Control", "public, max-age=5")
                        .build()
                } else {
                    // If offline, use cached data if available
                    originalRequest.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=604800")
                        .build()
                }

                chain.proceed(request)
            }
            .build()
    }

    //Checks if the device has an active network connection
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    //Provides a singleton instance of GoogleBooksApiService
    val googleBooksService: GoogleBooksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApiService::class.java)
    }
}