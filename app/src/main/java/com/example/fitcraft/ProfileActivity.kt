package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.content.SharedPreferences
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : Activity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvBirthdate: TextView
    private lateinit var tvGender: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)

        tvFirstName = findViewById(R.id.etFirstName)
        tvLastName = findViewById(R.id.etLastName)
        tvEmail = findViewById(R.id.etEmail)
        tvPhoneNumber = findViewById(R.id.etPhoneNumber)
        tvBirthdate = findViewById(R.id.etBirthdate)
        tvGender = findViewById(R.id.etGender)
        val btnEditProfile = findViewById<Button>(R.id.btnEdit)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        loadProfileData()

        btnEditProfile.setOnClickListener {
            Log.e("FitCraft Profile", "Edit is Clicked")
            val intent = Intent(this, ProfileEditActivity::class.java)
            startActivity(intent)
        }
        val homebutton = findViewById<ImageView>(R.id.homebutton)
        homebutton.setOnClickListener {
            Log.e("FitCraft Home", "Home is Clicked")


            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            Log.e("FitCraft Home", "Back is Clicked")
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun loadProfileData() {
        sharedPreferences.apply {
            tvFirstName.text = getString("firstName", "") ?: ""
            tvLastName.text = getString("lastName", "") ?: ""
            tvEmail.text = getString("email", "") ?: ""
            tvPhoneNumber.text = getString("phoneNumber", "") ?: ""
            tvBirthdate.text = getString("birthdate", "") ?: ""
            tvGender.text = getString("gender", "") ?: ""
        }
    }


}