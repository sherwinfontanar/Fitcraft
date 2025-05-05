package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONObject
import java.nio.charset.Charset
private val IMAGE_PICK_CODE = 1000
class UserProfileActivity : Activity() {

    private lateinit var firstName: EditText
    private lateinit var middleName: EditText
    private lateinit var lastName: EditText
    private lateinit var saveButton: Button
    private lateinit var editButton: Button
    private lateinit var profilePic: ImageView
    private lateinit var email: EditText
    private lateinit var phoneNumber: EditText

    private var isEditing = false
    private var profileExists = false

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageBase64: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Check if token exists
        if (Utility.token.isNullOrEmpty()) {
            Toast.makeText(this, "Not authenticated. Please login again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        firstName = findViewById(R.id.firstName)
        middleName = findViewById(R.id.middleName)
        lastName = findViewById(R.id.lastName)
        saveButton = findViewById(R.id.saveButton)
        editButton = findViewById(R.id.editButton)
        profilePic = findViewById(R.id.profilePic)
        email = findViewById(R.id.email)
        phoneNumber = findViewById(R.id.phoneNumber)

        setEditing(false)
        loadProfile()

        editButton.setOnClickListener {
            setEditing(true)
        }

        saveButton.setOnClickListener {
            if (isEditing) {
                saveProfile()
            }
        }



        val btnChangePhoto: ImageButton = findViewById(R.id.btnChangePhoto)
        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICK_CODE)
        }

    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setEditing(enabled: Boolean) {
        firstName.isEnabled = enabled
        middleName.isEnabled = enabled
        lastName.isEnabled = enabled
        phoneNumber.isEnabled = enabled
        email.isEnabled = enabled
        saveButton.isEnabled = enabled
        isEditing = enabled
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val uri = data.data
            profilePic.setImageURI(uri)

            // Convert to base64
            val inputStream = contentResolver.openInputStream(uri!!)
            val bytes = inputStream!!.readBytes()
            selectedImageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    }


    private fun loadProfile() {
        val url = "${Utility.apiUrl}/api/profile"
        Log.d("ProfileActivity", "Loading profile from: $url")
        Log.d("ProfileActivity", "Token is ${if (Utility.token.isNullOrEmpty()) "NULL or EMPTY" else "PRESENT"}")

        val request = object : JsonObjectRequest(
            Method.GET, url, null,
            { response ->
                Log.d("ProfileActivity", "Profile loaded: $response")
                firstName.setText(response.optString("firstName", ""))
                middleName.setText(response.optString("middleName", ""))
                lastName.setText(response.optString("lastName", ""))
                phoneNumber.setText(response.optString("phoneNumber", ""))
                email.setText(response.optString("email", ""))
                profileExists = true
                setEditing(false)
            },
            { error ->
                val errorMessage = getVolleyErrorMessage(error)
                Log.e("ProfileActivity", "Error loading profile: $errorMessage")

                if (error.networkResponse?.statusCode == 401 || error.networkResponse?.statusCode == 403) {
                    // Token is invalid or expired
                    Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    Utility.clearToken()
                    navigateToLogin()
                } else if (error.networkResponse?.statusCode == 404) {
                    // No profile exists yet
                    Toast.makeText(this, "No profile found. Please create one.", Toast.LENGTH_SHORT).show()
                    profileExists = false
                    setEditing(true)
                } else {
                    Toast.makeText(this, "Error loading profile: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                // Make sure token is properly formatted with Bearer prefix
                Utility.token?.let { token ->
                    headers["Authorization"] = if (token.startsWith("Bearer ")) token else "Bearer $token"
                }
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun saveProfile() {
        val method = if (profileExists) Request.Method.PUT else Request.Method.POST
        val url = "${Utility.apiUrl}/api/profile"

        val requestBody = JSONObject().apply {
            put("firstName", firstName.text.toString())
            put("middleName", middleName.text.toString())
            put("lastName", lastName.text.toString())
            put("email", email.text.toString())
            put("phoneNumber", phoneNumber.text.toString())
            selectedImageBase64?.let {
                put("profilePicture", it)
            }
        }

        Log.d("ProfileActivity", "Saving profile using method: ${if (method == Request.Method.PUT) "PUT" else "POST"}")
        Log.d("ProfileActivity", "Request body: $requestBody")

        val request = object : JsonObjectRequest(
            method, url, requestBody,
            { response ->
                Log.d("ProfileActivity", "Profile saved: $response")
                Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                profileExists = true
                setEditing(false)
            },
            { error ->
                val errorMessage = getVolleyErrorMessage(error)
                Log.e("ProfileActivity", "Error saving profile: $errorMessage")

                if (error.networkResponse?.statusCode == 401 || error.networkResponse?.statusCode == 403) {
                    // Token is invalid or expired
                    Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    Utility.clearToken()
                    navigateToLogin()
                } else if (error.networkResponse?.statusCode == 400 && method == Request.Method.POST) {
                    // If profile already exists, try updating instead
                    Log.d("ProfileActivity", "Profile exists, switching to PUT")
                    profileExists = true
                    saveProfile()
                } else {
                    Toast.makeText(this, "Error saving profile: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                // Make sure token is properly formatted with Bearer prefix
                Utility.token?.let { token ->
                    headers["Authorization"] = if (token.startsWith("Bearer ")) token else "Bearer $token"
                }
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun getVolleyErrorMessage(error: VolleyError): String {
        return when {
            error.networkResponse != null && error.networkResponse.data != null -> {
                try {
                    val errorBody = String(error.networkResponse.data, Charset.forName(HttpHeaderParser.parseCharset(error.networkResponse.headers)))
                    try {
                        val errorJson = JSONObject(errorBody)
                        errorJson.optString("message", errorBody)
                    } catch (e: Exception) {
                        errorBody
                    }
                } catch (e: Exception) {
                    "Error ${error.networkResponse.statusCode}"
                }
            }
            error.message != null -> error.message ?: "Unknown error"
            else -> "Network error"
        }
    }
}