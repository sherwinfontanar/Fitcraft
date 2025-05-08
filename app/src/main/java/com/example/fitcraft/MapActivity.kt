package com.example.fitcraft

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.fitcraft.TailorProfileActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.MapFragment
import java.io.IOException
import java.util.Locale

class MapActivity : Activity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userMarker: Marker? = null

    // Address type
    private var addressType: String = ""

    // For storing tailor addresses
    private val tailorAddresses = mutableListOf<TailorAddress>()

    // User address location
    private var userAddressLocation: LatLng? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Get address type from intent
        addressType = intent.getStringExtra("ADDRESS_TYPE") ?: "USER_ADDRESS"

        // Get user address from intent (if coming from checkout)
        val userAddress = intent.getStringExtra("USER_ADDRESS")

        when (addressType) {
            "TAILOR_ADDRESS" -> title = "Select Tailor Address"
            "NEARBY_TAILORS" -> title = "Nearby Tailoring Services"
            else -> title = "Select Your Address"
        }

        // Initialize map fragment
        val mapFragment = fragmentManager.findFragmentById(R.id.map_fragment) as MapFragment
        mapFragment.getMapAsync(this)

        // Location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val saveBtn = findViewById<Button>(R.id.btn_save_location)

        if (addressType == "NEARBY_TAILORS") {
            // Hide save button for nearby tailors view
            saveBtn.visibility = View.GONE

            // Load tailor addresses
            loadTailorAddressesFromDatabase()

            // Get user location from preferences if available
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val savedAddress = prefs.getString("saved_address", null)
            val savedLat = prefs.getFloat("saved_lat", 0f)
            val savedLng = prefs.getFloat("saved_lng", 0f)

            if (savedLat != 0f && savedLng != 0f) {
                userAddressLocation = LatLng(savedLat.toDouble(), savedLng.toDouble())
            } else if (userAddress != null) {
                // Try to geocode the user address
                geocodeAddress(userAddress)
            }
        } else {
            // Set appropriate save button text
            val saveBtnText = if (addressType == "TAILOR_ADDRESS") "Save Tailor Address" else "Save Your Address"
            saveBtn.text = saveBtnText
        }

        btnBack.setOnClickListener {
            when (addressType) {
                "TAILOR_ADDRESS" -> startActivity(Intent(this, TailorProfileActivity::class.java))
                "NEARBY_TAILORS" -> startActivity(Intent(this, Cart::class.java))
                else -> startActivity(Intent(this, UserProfileActivity::class.java))
            }
        }

        // Save address when button is clicked (not visible for NEARBY_TAILORS)
        saveBtn.setOnClickListener {
            saveSelectedAddress()
        }
    }

    private fun geocodeAddress(address: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                userAddressLocation = LatLng(addresses[0].latitude, addresses[0].longitude)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Enable map gestures
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        map.isMyLocationEnabled = true

        if (addressType == "NEARBY_TAILORS" && userAddressLocation != null) {
            // For nearby tailors mode with known user address, center on user address
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(userAddressLocation!!, 14f))
            showTailorPinsOnly(userAddressLocation!!)
        } else {
            // Set initial location if available
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    if (addressType == "NEARBY_TAILORS") {
                        // For nearby tailors mode, only show current location and tailor pins
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                        showTailorPinsOnly(userLatLng)
                    } else {
                        // For address selection mode
                        // Get the address for the initial location
                        reverseGeocodeLocation(userLatLng) { address ->
                            userMarker = map.addMarker(
                                MarkerOptions()
                                    .position(userLatLng)
                                    .title(address ?: "Your Location")
                            )
                            if (address != null) {
                                userMarker?.showInfoWindow()
                            }
                        }
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))

                        // Configure map click listener for address selection
                        setupMapClickForAddressSelection()
                    }
                } else {
                    Toast.makeText(this, "Couldn't fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun reverseGeocodeLocation(latLng: LatLng, callback: (String?) -> Unit) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val selectedAddress = addresses[0].getAddressLine(0)
                callback(selectedAddress)
            } else {
                callback(null)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            callback(null)
        }
    }

    private fun setupMapClickForAddressSelection() {
        val geocoder = Geocoder(this, Locale.getDefault())

        // Tap on map to select new location
        map.setOnMapClickListener { latLng ->
            userMarker?.remove()
            reverseGeocodeLocation(latLng) { address ->
                userMarker = map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(address ?: "Selected Location")
                )
                userMarker?.showInfoWindow()
                if (address != null) {
                    Toast.makeText(this, "Address: $address", Toast.LENGTH_LONG).show()
                }
            }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    private fun saveSelectedAddress() {
        val position = userMarker?.position
        val addressTitle = userMarker?.title

        if (position != null && addressTitle != null) {
            // Remove the condition checking if title is "Your Location"
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

            // Save to different keys based on type
            val addressKey = if (addressType == "TAILOR_ADDRESS") "saved_tailor_address" else "saved_address"
            val latKey = if (addressType == "TAILOR_ADDRESS") "saved_tailor_lat" else "saved_lat"
            val lngKey = if (addressType == "TAILOR_ADDRESS") "saved_tailor_lng" else "saved_lng"

            prefs.edit()
                .putString(addressKey, addressTitle)
                .putFloat(latKey, position.latitude.toFloat())
                .putFloat(lngKey, position.longitude.toFloat())
                .apply()

            Toast.makeText(this, "Location saved!", Toast.LENGTH_SHORT).show()

            // Navigate to the appropriate activity based on address type
            val intent = if (addressType == "TAILOR_ADDRESS") {
                Intent(this, TailorProfileActivity::class.java)
            } else {
                Intent(this, UserProfileActivity::class.java)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Please select a location first.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTailorAddressesFromDatabase() {
        // First, load saved tailor address from SharedPreferences
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val tailorAddress = prefs.getString("saved_tailor_address", null)
        val tailorLat = prefs.getFloat("saved_tailor_lat", 0f)
        val tailorLng = prefs.getFloat("saved_tailor_lng", 0f)

        if (tailorAddress != null && tailorLat != 0f && tailorLng != 0f) {
            tailorAddresses.add(TailorAddress(
                name = "Sample Tailor Shop",
                address = tailorAddress,
                location = LatLng(tailorLat.toDouble(), tailorLng.toDouble())
            ))
        }

        val geocoder = Geocoder(this, Locale.getDefault())

        // Try to geocode addresses to get coordinates
        try {
            // Add more sample tailors
            val tailor1Address = "123 Fashion St, San Francisco, CA"
            val tailor1Addresses = geocoder.getFromLocationName(tailor1Address, 1)
            if (!tailor1Addresses.isNullOrEmpty()) {
                val location = LatLng(tailor1Addresses[0].latitude, tailor1Addresses[0].longitude)
                tailorAddresses.add(TailorAddress("Expert Tailoring", tailor1Address, location))
            } else {
                // Fallback to example coordinates
                tailorAddresses.add(TailorAddress(
                    "Expert Tailoring",
                    "123 Fashion St, San Francisco, CA",
                    LatLng(37.7749, -122.4194)
                ))
            }

            val tailor2Address = "456 Design Ave, San Francisco, CA"
            val tailor2Addresses = geocoder.getFromLocationName(tailor2Address, 1)
            if (!tailor2Addresses.isNullOrEmpty()) {
                val location = LatLng(tailor2Addresses[0].latitude, tailor2Addresses[0].longitude)
                tailorAddresses.add(TailorAddress("Stitch Perfect", tailor2Address, location))
            } else {
                // Fallback to example coordinates
                tailorAddresses.add(TailorAddress(
                    "Stitch Perfect",
                    "456 Design Ave, San Francisco, CA",
                    LatLng(37.7850, -122.4100)
                ))
            }

            val tailor3Address = "789 Fabric Blvd, San Francisco, CA"
            tailorAddresses.add(TailorAddress(
                "Custom Creations",
                tailor3Address,
                LatLng(37.7695, -122.4250)
            ))

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun showTailorPinsOnly(userLocation: LatLng) {
        // Display just the pins for tailors on the map
        for (tailor in tailorAddresses) {
            map.addMarker(
                MarkerOptions()
                    .position(tailor.location)
                    .title(tailor.name)
            )
        }

        // Set up marker click listener
        map.setOnMarkerClickListener { marker ->
            // Navigate to proper activity based on address type
            val selectedTailor = tailorAddresses.find { it.name == marker.title }
            if (selectedTailor != null) {
                val intent = Intent(this, TailorInfoActivity::class.java)
                intent.putExtra("TAILOR_NAME", selectedTailor.name)
                intent.putExtra("TAILOR_ADDRESS", selectedTailor.address)
                intent.putExtra("TAILOR_LAT", selectedTailor.location.latitude)
                intent.putExtra("TAILOR_LNG", selectedTailor.location.longitude)
                startActivity(intent)
                true // Consume the event
            } else {
                false // Allow default behavior
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate() // Permission granted, recreate to trigger map setup
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // Data class for storing tailor information
    data class TailorAddress(
        val name: String,
        val address: String,
        val location: LatLng
    )
}