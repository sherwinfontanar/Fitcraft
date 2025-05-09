package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val firstName = findViewById<EditText>(R.id.etFirstName)
        val lastName = findViewById<EditText>(R.id.etLastName)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnShowPassword = findViewById<ImageView>(R.id.btnShowPassword)
        val btnShowCPassword = findViewById<ImageView>(R.id.btnShowCPassword)

        // Set up the spinner
        val roles = arrayOf("Customer", "Tailor")
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, roles) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(resources.getColor(R.color.one))
                textView.textSize = 14f
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(resources.getColor(R.color.one))
                textView.textSize = 14f
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // Back button click listener
        btnBack.setOnClickListener {
            finish()
        }

        // Password visibility toggle
        btnShowPassword.setOnClickListener {
            togglePasswordVisibility(password, btnShowPassword)
        }

        btnShowCPassword.setOnClickListener {
            togglePasswordVisibility(confirmPassword, btnShowCPassword)
        }

        registerButton.setOnClickListener {
            if (email.text.toString().isEmpty() || 
                password.text.toString().isEmpty() || 
                confirmPassword.text.toString().isEmpty() ||
                firstName.text.toString().isEmpty() ||
                lastName.text.toString().isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.text.toString() != confirmPassword.text.toString()) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(
                email.text.toString(),
                password.text.toString(),
                spinnerRole.selectedItem.toString(),
                firstName.text.toString(),
                lastName.text.toString()
            )
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

    private fun registerUser(email: String, password: String, role: String, firstName: String, lastName: String) {
        val url = "${Utility.apiUrl}/api/register"

        val requestBody = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("role", role)
            put("firstName", firstName)
            put("lastName", lastName)
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