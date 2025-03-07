// MainActivity.kt
package com.example.storyhive

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.storyhive.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.squareup.picasso.Picasso

class
MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Enable Picasso logging for debugging image loading
            Picasso.get().setLoggingEnabled(true)

            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d("MainActivity", "Starting initialization")

            // Initialize Firebase Firestore with local persistence enabled
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Enables offline caching
                .build()
            firestore.firestoreSettings = settings
            Log.d("MainActivity", "Firestore initialized")

            // Initialize Navigation Component
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            Log.d("MainActivity", "Navigation initialized")

            // Check if the app is using StoryHiveApplication
            val application = application
            if (application is StoryHiveApplication) {
                Log.d("MainActivity", "Using StoryHiveApplication")

                // Ensure that the database and image cache manager are initialized
                val database = application.database
                Log.d("MainActivity", "Database accessed")

                val imageCacheManager = application.imageCacheManager
                Log.d("MainActivity", "ImageCacheManager accessed")

                val postRepository = application.postRepository
                Log.d("MainActivity", "PostRepository accessed")
            } else {
                Log.w("MainActivity", "Application is not StoryHiveApplication")
            }

            // Show/hide the BottomNavigationView based on the current screen
            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.welcomeFragment, R.id.loginFragment, R.id.signUpFragment -> {
                        binding.bottomNavigationView.visibility = View.GONE
                        supportActionBar?.hide()
                    }

                    else -> {
                        if (auth.currentUser == null) {
                            // If user is not logged in, navigate back to the welcome screen
                            navController.navigate(R.id.welcomeFragment)
                        } else {
                            // Show BottomNavigationView and action bar for main screens
                            binding.bottomNavigationView.visibility = View.VISIBLE
                            supportActionBar?.show()
                        }
                    }
                }
            }

            // Connect BottomNavigationView with Navigation Controller
            binding.bottomNavigationView.setupWithNavController(navController)

            Log.d("MainActivity", "Setup completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization", e)
            Toast.makeText(this, "Error initializing the app. Please try again", Toast.LENGTH_LONG)
                .show()
        }
    }
}
