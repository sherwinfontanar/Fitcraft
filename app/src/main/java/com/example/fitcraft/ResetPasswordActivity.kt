package com.example.fitcraft

import android.app.Activity
import android.content.Intent
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
    private lateinit var token: String
    private lateinit var email: String
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        newPasswordEditText = findViewById(R.id.etNewPassword)
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword)
        resetButton = findViewById(R.id.btnUpdatePassword)

        // Extract token and email from deep link
        val data = intent?.data
        if (data != null) {
            token = data.getQueryParameter("token").orEmpty()
            email = data.getQueryParameter("email").orEmpty()
        }

        resetButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(email, newPassword, token)
        }
    }

    private fun resetPassword(email: String, newPassword: String, token: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "${Utility.apiUrl}/api/reset-password"

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("newPassword", newPassword)
            put("resetToken", token)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                Toast.makeText(this, "Password reset successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }
}