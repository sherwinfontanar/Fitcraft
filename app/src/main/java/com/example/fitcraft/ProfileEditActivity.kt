package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileEditActivity : Activity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etBirthdate = findViewById<EditText>(R.id.etBirthdate)
        val etGender = findViewById<EditText>(R.id.etGender)
        val btnSaveChanges = findViewById<Button>(R.id.btnSaveChanges)
        val backlanding = findViewById<ImageButton>(R.id.btnBack)

        // Load saved profile data, but keep XML defaults if not available
        etFirstName.setText(sharedPreferences.getString("firstName", etFirstName.text.toString()))
        etLastName.setText(sharedPreferences.getString("lastName", etLastName.text.toString()))
        etEmail.setText(sharedPreferences.getString("email", etEmail.text.toString()))
        etPhoneNumber.setText(sharedPreferences.getString("phoneNumber", etPhoneNumber.text.toString()))
        etBirthdate.setText(sharedPreferences.getString("birthdate", etBirthdate.text.toString()))
        etGender.setText(sharedPreferences.getString("gender", etGender.text.toString()))

        // Save changes and update ProfileActivity
        btnSaveChanges.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putString("firstName", etFirstName.text.toString())
            editor.putString("lastName", etLastName.text.toString())
            editor.putString("email", etEmail.text.toString())
            editor.putString("phoneNumber", etPhoneNumber.text.toString())
            editor.putString("birthdate", etBirthdate.text.toString())
            editor.putString("gender", etGender.text.toString())
            editor.apply()

            Log.d("FitCraft Profile", "Profile updated successfully!")

            // Return to ProfileActivity and refresh UI
            val intent = Intent(this, ProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        backlanding.setOnClickListener {
            Log.e("FitCraft Home", "Back to Landing Page")
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
        }
    }
}