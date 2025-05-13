package com.example.fitcraft

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.GeocoderHelper
import com.example.fitcraft.utils.Utility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.*

private val IMAGE_PICK_CODE = 1000
private val LOCATION_PERMISSION_REQUEST_CODE = 2000
private val MAP_ACTIVITY_REQUEST_CODE = 3000

class UserProfileActivity : Activity() {
    val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var firstName: EditText
    private lateinit var middleName: EditText
    private lateinit var lastName: EditText
    private lateinit var saveButton: Button
    private lateinit var editButton: Button
    private lateinit var profilePic: ImageView
    private lateinit var email: EditText
    private lateinit var phoneNumber: EditText

    private lateinit var province: EditText
    private lateinit var city: EditText
    private lateinit var barangay: EditText
    private lateinit var sitio: EditText
    private lateinit var street: EditText
    private lateinit var building: EditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isEditing = false
    private var profileExists = false

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageBase64: String? = null


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)


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


        province = findViewById(R.id.province)
        city = findViewById(R.id.city)
        barangay = findViewById(R.id.barangay)
        sitio = findViewById(R.id.sitio)
        street = findViewById(R.id.street)
        building = findViewById(R.id.building)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setEditing(false)
        loadProfile()


        loadAddressData()

        editButton.setOnClickListener {
            setEditing(true)
        }

        saveButton.setOnClickListener {
            if (isEditing) {
                saveProfile()
            } else {

                setEditing(false)
                loadProfile()
            }
        }


        val btnChangePhoto: ImageButton = findViewById(R.id.btnChangePhoto)
        btnChangePhoto.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        }

        val useLocationButton = findViewById<Button>(R.id.useLocationButton)

        useLocationButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)

            intent.putExtra("ADDRESS_TYPE", "USER_ADDRESS")
            startActivityForResult(intent, MAP_ACTIVITY_REQUEST_CODE)
        }


        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
        }
    }


    private fun loadAddressData() {

        val intentHasAddress = intent.hasExtra("province") ||
                intent.hasExtra("city") ||
                intent.hasExtra("barangay")

        if (intentHasAddress) {

            province.setText(intent.getStringExtra("province") ?: "")
            city.setText(intent.getStringExtra("city") ?: "")
            barangay.setText(intent.getStringExtra("barangay") ?: "")
            sitio.setText(intent.getStringExtra("sitio") ?: "")
            street.setText(intent.getStringExtra("street") ?: "")
            building.setText(intent.getStringExtra("buildingNumber") ?: "")
            Log.d("UserProfileActivity", "Loaded address data from intent extras")

            if (isEditing) {
                Toast.makeText(this, "Address data loaded from map", Toast.LENGTH_SHORT).show()
            }
        } else {

            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val hasAddressInPrefs = prefs.contains("user_address_province") ||
                    prefs.contains("user_address_city") ||
                    prefs.contains("user_address_barangay")

            if (hasAddressInPrefs) {
                province.setText(prefs.getString("user_address_province", "") ?: "")
                city.setText(prefs.getString("user_address_city", "") ?: "")
                barangay.setText(prefs.getString("user_address_barangay", "") ?: "")
                sitio.setText(prefs.getString("user_address_sitio", "") ?: "")
                street.setText(prefs.getString("user_address_street", "") ?: "")
                building.setText(prefs.getString("user_address_buildingNumber", "") ?: "")
                Log.d("UserProfileActivity", "Loaded address data from SharedPreferences")


                if (isEditing) {
                    Toast.makeText(this, "Address data loaded from saved location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {

                    val geocoderHelper = GeocoderHelper(this)
                    val addressComponents = geocoderHelper.getDetailedAddressFromLatLng(
                        it.latitude,
                        it.longitude
                    )

                    // Fill in the address fields from the structured components
                    province.setText(addressComponents["province"] ?: "")
                    city.setText(addressComponents["city"] ?: "")
                    barangay.setText(addressComponents["barangay"] ?: "")
                    sitio.setText(addressComponents["sitio"] ?: "")
                    street.setText(addressComponents["street"] ?: "")
                    building.setText(addressComponents["buildingNumber"] ?: "")

                    Toast.makeText(this, "Address updated from location", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("UserProfileActivity", "Error getting location", e)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLastLocation()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
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


        province.isEnabled = enabled
        city.isEnabled = enabled
        barangay.isEnabled = enabled
        sitio.isEnabled = enabled
        street.isEnabled = enabled
        building.isEnabled = enabled


        if (enabled) {
            saveButton.text = "Save"
            editButton.isEnabled = false
        } else {
            saveButton.text = "Cancel"
            editButton.isEnabled = true
        }

        isEditing = enabled
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            profilePic.setImageBitmap(imageBitmap) // Set the image in ImageView


            try {
                val baos = java.io.ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val byteArray = baos.toByteArray()
                selectedImageBase64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e("UserProfileActivity", "Error converting image to base64", e)
            }
        }
        else if (requestCode == MAP_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            if (data != null) {

                if (data.hasExtra("province") || data.hasExtra("city")) {
                    province.setText(data.getStringExtra("province") ?: "")
                    city.setText(data.getStringExtra("city") ?: "")
                    barangay.setText(data.getStringExtra("barangay") ?: "")
                    sitio.setText(data.getStringExtra("sitio") ?: "")
                    street.setText(data.getStringExtra("street") ?: "")
                    building.setText(data.getStringExtra("buildingNumber") ?: "")


                    if (!isEditing) {
                        setEditing(true)
                    }

                    Toast.makeText(this, "Address updated from map", Toast.LENGTH_SHORT).show()
                } else {

                    loadAddressData()
                }
            }
        }
    }

    private fun loadProfile() {
        val url = "${Utility.apiUrl}/api/profile"
        Log.d("UserProfileActivity", "Loading profile from: $url")
        Log.d("UserProfileActivity", "Token is ${if (Utility.token.isNullOrEmpty()) "NULL or EMPTY" else "PRESENT"}")

        val request = object : JsonObjectRequest(
            Method.GET, url, null,
            { response ->
                Log.d("UserProfileActivity", "Profile loaded: $response")
                firstName.setText(response.optString("firstName", ""))
                middleName.setText(response.optString("middleName", ""))
                lastName.setText(response.optString("lastName", ""))
                phoneNumber.setText(response.optString("phoneNumber", ""))
                email.setText(response.optString("email", ""))

                // Load address information
                province.setText(response.optString("province", ""))
                city.setText(response.optString("city", ""))
                barangay.setText(response.optString("barangay", ""))
                sitio.setText(response.optString("sitio", ""))
                street.setText(response.optString("street", ""))
                building.setText(response.optString("buildingNumber", ""))

                profileExists = true
                setEditing(false)

                loadAddressData()
            },
            { error ->
                val errorMessage = getVolleyErrorMessage(error)
                Log.e("ProfileActivity", "Error loading profile: $errorMessage")

                if (error.networkResponse?.statusCode == 401 || error.networkResponse?.statusCode == 403) {
                    Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    Utility.clearToken()
                    navigateToLogin()
                } else if (error.networkResponse?.statusCode == 404) {
                    Toast.makeText(this, "No profile found. Please create one.", Toast.LENGTH_SHORT).show()
                    profileExists = false
                    setEditing(true)

                    loadAddressData()
                } else {
                    Toast.makeText(this, "Error loading profile: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
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

            // Add address information
            //put("province", province.text.toString())
            //put("city", city.text.toString())
            //put("barangay", barangay.text.toString())
            //put("sitio", sitio.text.toString())
            //put("street", street.text.toString())
            //put("buildingNumber", building.text.toString())


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
                    Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                    Utility.clearToken()
                    navigateToLogin()
                } else if (error.networkResponse?.statusCode == 400 && method == Request.Method.POST) {
                    Log.d("ProfileActivity", "Profile exists, switching to PUT")
                    profileExists = true
                    saveProfile()
                } else {
                    Toast.makeText(this, "Error saving profiledddss: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
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