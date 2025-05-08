package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Cart : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        // Find the checkout button
        val checkoutButton = findViewById<Button>(R.id.btnCheckout)

        // Set click listener for checkout button
        checkoutButton.setOnClickListener {
            // Get user address from Profile
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val userAddress = prefs.getString("saved_address", null)

            // Redirect to map activity showing nearby tailoring services pins only
            val intent = Intent(this, MapActivity::class.java)
            //val intent = Intent(this, CheckoutActivity::class.java)
            intent.putExtra("ADDRESS_TYPE", "NEARBY_TAILORS")

            // Pass user address if available
            //if (userAddress != null) {
                //intent.putExtra("USER_ADDRESS", userAddress)
            //}

            startActivity(intent)
        }

        val checkboxItem = findViewById<CheckBox>(R.id.checkboxItem1)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)

        val updateTotal = {
            var total = 0
            if (checkboxItem.isChecked) total += 890
            tvTotal.text = "Total ₱$total"
        }

        checkboxItem.setOnCheckedChangeListener { _, _ -> updateTotal() }

        // Initialize total on startup
        updateTotal()

    }
}