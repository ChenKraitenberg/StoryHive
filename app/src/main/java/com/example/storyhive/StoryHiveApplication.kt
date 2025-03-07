package com.example.storyhive

import android.app.Application
import android.util.Log
import com.example.storyhive.data.local.ImageCacheManager
import com.example.storyhive.data.local.StoryHiveDatabase
import com.example.storyhive.data.network.NetworkConnectivityManager
import com.example.storyhive.data.network.RetrofitClient
import com.example.storyhive.repository.FirebaseRepository
import com.example.storyhive.repository.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * StoryHiveApplication: The main Application class for initializing global dependencies.
 * This class handles database initialization, network monitoring, offline caching,
 * and background cleanup operations.
 */

class StoryHiveApplication : Application() {
    // ApplicationScope for background tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lazy initialization of the local database
    val database by lazy { StoryHiveDatabase.getInstance(this) }

    // Lazy initialization of the image cache manager
    val imageCacheManager by lazy {
        ImageCacheManager.getInstance(this, database.imageCacheDao())
    }

    // Network connectivity manager to track internet status
    val networkManager by lazy {
        NetworkConnectivityManager(this)
    }

    // Post repository with offline support for handling posts and comments
    val postRepository by lazy {
        PostRepository.getInstance().apply {
            initOfflineMode(database.postDao(), database.commentDao())
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Retrofit client for API requests
        RetrofitClient.init(this)

        // Start network monitoring for connectivity changes
        networkManager.startNetworkCallback()

        // Perform cleanup and data synchronization on app startup
        applicationScope.launch {
            try {
                // Clean up old cached images
                imageCacheManager.cleanupCache()

                // Synchronize user profile images stored in Firestore with local cache
                FirebaseRepository.syncUserProfileImages(imageCacheManager)

                // If connected to the internet, synchronize any pending offline data
                if (networkManager.isCurrentlyConnected()) {
                    postRepository.syncPendingData()
                }
            } catch (e: Exception) {
                Log.e("StoryHiveApplication", "Error during startup", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Stop network monitoring when the app is terminated
        networkManager.stopNetworkCallback()
    }
}