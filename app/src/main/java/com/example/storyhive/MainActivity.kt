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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // הגדרת ה-Toolbar
        setSupportActionBar(binding.toolbar)

        // הגדרת ה-NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // מחבר את ה-Toolbar לניווט
        setupActionBarWithNavController(navController)

        // מחבר את ה-BottomNavigationView ל-NavController
        binding.bottomNavigationView.apply {
            // קישור ל-NavController
            setupWithNavController(navController)
        }

        // מסתיר/מציג את ה-Toolbar בהתאם למסך
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.welcomeFragment) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
                // מפעיל את כפתור החזרה
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setDisplayShowHomeEnabled(true)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
