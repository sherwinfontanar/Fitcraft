package com.example.fitcraft.utils

import android.content.Context
import android.content.SharedPreferences

object Utility {
    // Base URL of your API
    const val apiUrl = "http://10.0.2.2:5000" // Change to your actual API URL
    // const val apiUrl = "http://192.168.1.29:5000"
    // For storing token in memory temporarily
    private var _token: String? = null

    // For storing token persistently
    private lateinit var prefs: SharedPreferences

    // Initialize with context
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("FitCraftPrefs", Context.MODE_PRIVATE)
        // Load any saved token
        _token = prefs.getString("auth_token", null)
    }

    // Token property with getter and setter
    var token: String?
        get() = _token
        set(value) {
            _token = value
            // Also save to SharedPreferences for persistence
            if (::prefs.isInitialized) {
                prefs.edit().putString("auth_token", value).apply()
            }
        }

    // Clear token on logout
    fun clearToken() {
        _token = null
        if (::prefs.isInitialized) {
            prefs.edit().remove("auth_token").apply()
        }
    }
}