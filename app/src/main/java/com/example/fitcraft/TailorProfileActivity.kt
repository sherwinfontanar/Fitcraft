package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TailorProfileActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tailor_profile)

        val useLocationButton = findViewById<Button>(R.id.btn_use_current_location)
        useLocationButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)

            intent.putExtra("ADDRESS_TYPE", "TAILOR_ADDRESS")
            startActivity(intent)
        }

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val savedAddress = prefs.getString("saved_tailor_address", "Address has not been set")

        val tvAddress = findViewById<TextView>(R.id.tv_address)
        tvAddress.text = savedAddress

    }
}