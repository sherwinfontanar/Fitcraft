package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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


                // You can now use token to access protected routes

                // TODO: Save token to SharedPreferences if needed
                if (role == "tailor") {
                    Log.d(TAG, "Login successful: Token: $token Role: $role")
                    Toast.makeText(this, "Login successful! Role: $role", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, TailorDashboardActivity::class.java))
                } else if (role == "user"){
                    startActivity(Intent(this, MeasurementActivity::class.java))
                }

            },
            { error ->
                Log.e(TAG, "Login error: ${error.message}")
                Toast.makeText(this, "Login failed. Check your credentials.", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }
}
