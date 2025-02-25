package com.example.carbonwise

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.carbonwise.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isUserLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val storedToken = getToken(this)
        isUserLoggedIn = !storedToken.isNullOrEmpty()

        setupNavigation()
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val navGraphResId = if (isUserLoggedIn) {
            R.navigation.mobile_navigation_user
        } else {
            R.navigation.mobile_navigation_guest
        }

        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(navGraphResId)
        navController.graph = navGraph

        // Setup bottom navigation based on login state
        binding.navView.menu.clear()
        val bottomMenuResId = if (isUserLoggedIn) {
            R.menu.bottom_nav_menu_user
        } else {
            R.menu.bottom_nav_menu_guest
        }
        binding.navView.inflateMenu(bottomMenuResId)
        binding.navView.setupWithNavController(navController)

        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_scan -> {
                    val navController = findNavController(R.id.nav_host_fragment_activity_main)
                    navController.popBackStack(R.id.navigation_scan, true)
                    navController.navigate(R.id.navigation_scan)
                    true
                }
                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }
    }

    fun switchToLoggedInMode() {
        isUserLoggedIn = true
        saveToken(this, "dummy_token")

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.mobile_navigation_user)
        navController.graph = navGraph

        // Update the bottom navigation menu
        binding.navView.menu.clear()
        binding.navView.inflateMenu(R.menu.bottom_nav_menu_user)
        binding.navView.setupWithNavController(navController)
    }

    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val TOKEN_KEY = "google_id_token"

        fun saveToken(context: Context, token: String) {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(TOKEN_KEY, token)
                apply()
            }
        }

        fun getToken(context: Context): String? {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sharedPref.getString(TOKEN_KEY, null)
        }

        fun clearToken(context: Context) {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                remove(TOKEN_KEY)
                apply()
            }
        }
    }
}
