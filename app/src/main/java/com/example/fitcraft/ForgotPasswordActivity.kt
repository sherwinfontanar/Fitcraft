package com.example.fitcraft

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONObject

class ForgotPasswordActivity : Activity() {
    private val TAG = "ForgotPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val email = findViewById<EditText>(R.id.etEmail)
        val resetButton = findViewById<Button>(R.id.btnResetPassword)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            if (email.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(email.text.toString())
        }
    }

    private fun resetPassword(email: String) {
        val url = "${Utility.apiUrl}/api/forgot-password"

        val requestBody = JSONObject().apply {
            put("email", email)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                Log.d(TAG, "Reset password request successful: $response")
                Toast.makeText(this, "Reset password link sent to your email", Toast.LENGTH_LONG).show()
                finish()
            },
            { error ->
                Log.e(TAG, "Reset password error: ${error.message}")
                val errorMessage = when {
                    error.networkResponse != null && error.networkResponse.data != null -> {
                        try {
                            val errorJson = JSONObject(String(error.networkResponse.data))
                            errorJson.optString("message", "Failed to send reset link")
                        } catch (e: Exception) {
                            "Failed to send reset link"
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