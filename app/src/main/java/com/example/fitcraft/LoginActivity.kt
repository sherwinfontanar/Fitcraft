package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONObject

class LoginActivity : Activity() {
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnSignIn)
        val registerButton = findViewById<TextView>(R.id.btnRegister)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnShowPassword = findViewById<ImageView>(R.id.btnShowPassword)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnBack.setOnClickListener {
            finish()
        }

        btnShowPassword.setOnClickListener {
            togglePasswordVisibility(passwordInput, btnShowPassword)
        }

        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun togglePasswordVisibility(editText: EditText, button: ImageView) {
        if (editText.inputType == InputType.TYPE_CLASS_TEXT) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setImageResource(R.drawable.eye)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT
            button.setImageResource(R.drawable.eye_off)
        }
        editText.setSelection(editText.text.length)
    }

    private fun loginUser(email: String, password: String) {
        val url = "${Utility.apiUrl}/api/login"

        val requestBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                val token = response.optString("token")
                Utility.token = token
                val role = response.getJSONObject("user").optString("role")

                val intent = when (role) {
                    "user" -> Intent(this, UserProfileActivity::class.java)
                    "tailor" -> Intent(this, TailorDashboardActivity::class.java)
                    else -> Intent(this, LoginActivity::class.java)
                }
                startActivity(intent)
                finish()
            },
            { error ->
                Log.e(TAG, "Login error: ${error.message}")
                val errorMessage = when {
                    error.networkResponse != null && error.networkResponse.data != null -> {
                        try {
                            val errorJson = JSONObject(String(error.networkResponse.data))
                            errorJson.optString("message", "Login failed")
                        } catch (e: Exception) {
                            "Login failed"
                        }
                    }
                    else -> "Network error. Please try again."
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }
}
