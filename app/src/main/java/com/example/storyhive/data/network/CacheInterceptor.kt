package com.example.storyhive.data.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.CacheControl
import java.util.concurrent.TimeUnit

class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        val cacheControl = CacheControl.Builder()
            .maxAge(7, TimeUnit.DAYS)
            .build()

        return response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
}