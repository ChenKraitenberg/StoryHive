// MainActivity.kt
package com.example.storyhive

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.storyhive.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        Picasso.get().setLoggingEnabled(true) // הפעלת לוגים של Picasso

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // מסתיר/מציג את ה-BottomNav בהתאם למסך
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.welcomeFragment, R.id.loginFragment, R.id.signUpFragment -> {
                    binding.bottomNavigationView.visibility = View.GONE
                    supportActionBar?.hide()
                }
                else -> {
                    if (auth.currentUser == null) {
                        // אם המשתמש לא מחובר, חזור למסך הכניסה
                        navController.navigate(R.id.welcomeFragment)
                    } else {
                        binding.bottomNavigationView.visibility = View.VISIBLE
                        supportActionBar?.show()
                    }
                }
            }
        }

        // מחבר את ה-BottomNav לניווט
        binding.bottomNavigationView.setupWithNavController(navController)
    }
}
