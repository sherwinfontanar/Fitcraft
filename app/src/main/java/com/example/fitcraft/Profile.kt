package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Profile : Activity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val useLocationButton = findViewById<Button>(R.id.btn_use_current_location1)
        useLocationButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val savedAddress = prefs.getString("saved_address", "Address has not been set")

        val tvAddress = findViewById<TextView>(R.id.tv_address1)
        tvAddress.text = savedAddress

        val cartB = findViewById<ImageView>(R.id.btn_cart1)
        cartB.setOnClickListener {
            // Redirect to map activity showing nearby tailoring services
            val intent = Intent(this, Cart::class.java)
            startActivity(intent)
        }

    }
}