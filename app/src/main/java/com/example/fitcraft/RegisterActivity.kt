package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONObject

class RegisterActivity : Activity() {
    private val TAG = "RegisterActivity"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.etConfirmPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val role = findViewById<EditText>(R.id.etRole)

        registerButton.setOnClickListener {
            if (email.text.toString().isEmpty() || password.text.toString().isEmpty() || role.text.toString().isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email.text.toString(), password.text.toString(), role.text.toString())
        }
    }

    private fun registerUser(email: String, password: String, role: String) {
        val url = "${Utility.apiUrl}/api/register"

        val requestBody = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("role", role)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                Log.d(TAG, "Registration successful: $response")
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            },
            { error ->
                Log.e(TAG, "Registration error: ${error.message}")
                val errorMessage = when {
                    error.networkResponse != null && error.networkResponse.data != null -> {
                        try {
                            val errorJson = JSONObject(String(error.networkResponse.data))
                            errorJson.optString("message", "Registration failed")
                        } catch (e: Exception) {
                            "Registration failed"
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