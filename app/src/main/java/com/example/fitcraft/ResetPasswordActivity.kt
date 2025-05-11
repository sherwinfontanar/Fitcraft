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

class ResetPasswordActivity : Activity() {
    private val TAG = "ResetPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val token = intent.getStringExtra("token")
        if (token == null) {
            Toast.makeText(this, "Invalid reset link", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val newPassword = findViewById<EditText>(R.id.etNewPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val updateButton = findViewById<Button>(R.id.btnUpdatePassword)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        updateButton.setOnClickListener {
            val password = newPassword.text.toString()
            val confirm = confirmPassword.text.toString()

            when {
                password.isEmpty() -> {
                    Toast.makeText(this, "Please enter new password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                confirm.isEmpty() -> {
                    Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password != confirm -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password.length < 8 -> {
                    Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            updatePassword(token, password)
        }
    }

    private fun updatePassword(token: String, newPassword: String) {
        val url = "${Utility.apiUrl}/api/reset-password"

        val requestBody = JSONObject().apply {
            put("token", token)
            put("newPassword", newPassword)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                Log.d(TAG, "Password reset successful: $response")
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_LONG).show()
                finish()
            },
            { error ->
                Log.e(TAG, "Password reset error: ${error.message}")
                val errorMessage = when {
                    error.networkResponse != null && error.networkResponse.data != null -> {
                        try {
                            val errorJson = JSONObject(String(error.networkResponse.data))
                            errorJson.optString("message", "Failed to update password")
                        } catch (e: Exception) {
                            "Failed to update password"
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