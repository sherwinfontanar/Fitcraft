package com.example.fitcraft

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONException
import org.json.JSONObject
import android.util.Base64

class LandingActivity : Activity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private val productsList = mutableListOf<TailorDashboardActivity.Product>()
    private lateinit var profileNameTextView: TextView
    private lateinit var bodyShapeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewProducts)
        profileNameTextView = findViewById(R.id.profilename)
        bodyShapeTextView = findViewById(R.id.txt_body_shape)

        // Setup components
        setupRecyclerView()
        setupNavigation()

        // Extract user info from token or fetch it
        fetchUserInfoFromServer()
    }

    override fun onResume() {
        super.onResume()
        fetchProducts()
        fetchUserInfoFromServer()// Refresh user info when returning to this activity
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(this, productsList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = productsAdapter
    }

    /**
     * Extracts user information directly from the JWT token
     * Most JWT tokens contain user information in the payload
     */

    /**
     * Fix padding in base64 encoded JWT payload
     */
    private fun normalizeBase64(input: String): String {
        // Add padding if needed
        var result = input
        while (result.length % 4 != 0) {
            result += "="
        }
        return result
    }

    /**
     * Fetch user info from an existing endpoint if available
     */
    private fun fetchUserInfoFromServer() {
        // This assumes you have an endpoint that returns the logged-in user's profile
        // Replace with whatever endpoint you actually have available in your existing API

        val url = "${Utility.apiUrl}/api/profile" // Adjust this URL to match your existing API

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    // Extract user data - adjust field names based on your actual API response
                    val firstName = response.getString("firstName")

                    // Update UI
                    profileNameTextView.text = firstName

                    // Update body shape if available
                    if (response.has("bodyType") && !response.isNull("bodyType")) {
                        bodyShapeTextView.text = "Body Shape: ${response.getString("bodyType")}"
                    }

                    Log.d("LandingActivity", "Updated user info from API: $firstName")
                } catch (e: JSONException) {
                    Log.e("LandingActivity", "Error parsing profile data: ${e.message}")
                }
            },
            { error ->
                Log.e("LandingActivity", "Error fetching profile: ${error.message}")

                // If unauthorized, redirect to login
                if (error.networkResponse?.statusCode == 401) {
                    Utility.clearToken()
                    redirectToLogin()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${Utility.token}"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun fetchProducts() {
        val url = "${Utility.apiUrl}/api/products"
        Log.d("LandingActivity", "Fetching products from: $url")

        val request = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                productsList.clear()
                for (i in 0 until response.length()) {
                    val productJson = response.getJSONObject(i)
                    val product = TailorDashboardActivity.Product(
                        id = productJson.getString("_id"),
                        name = productJson.getString("productName"),
                        price = productJson.getDouble("productPrice"),
                        color = productJson.getString("productColor"),
                        bodyType = productJson.getString("bodyType"),
                        description = productJson.getString("productDescription"),
                        imageUrl = productJson.getString("productImage")
                    )
                    productsList.add(product)
                }
                productsAdapter.notifyDataSetChanged()
            },
            { error ->
                Log.e("LandingActivity", "Error: ${error.message}")
                Toast.makeText(this, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${Utility.token ?: ""}"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun setupNavigation() {
        findViewById<ImageView>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageView>(R.id.ivProfilePicture).setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        findViewById<TextView>(R.id.profilename).setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        findViewById<LinearLayout>(R.id.measurement).setOnClickListener {
            startActivity(Intent(this, MeasurementActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.cartbutton).setOnClickListener {
            startActivity(Intent(this, Cart::class.java))
        }

        findViewById<LinearLayout>(R.id.homebutton).setOnClickListener {
            startActivity(Intent(this, LandingActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.logoutbutton).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_logout_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_confirm_logout).setOnClickListener {
            dialog.dismiss()
            // Clear token on logout
            Utility.clearToken()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}