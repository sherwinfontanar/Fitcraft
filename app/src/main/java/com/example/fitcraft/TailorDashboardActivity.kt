package com.example.fitcraft

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONArray

class TailorDashboardActivity : Activity() {

    private lateinit var uploadProductButton: Button
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private val productsList = mutableListOf<Product>()
    private val TAG = "TailorDashboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tailor_dashboard)

        // Initialize views
        uploadProductButton = findViewById(R.id.uploadProductButton)
        productsRecyclerView = findViewById(R.id.productsRecyclerView)


        // Set click listener for upload button
        uploadProductButton.setOnClickListener {
            val intent = Intent(this, ProductUploadActivity::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.logoutbutton).setOnClickListener {
            showLogoutDialog()
        }

        // Initialize the products adapter
        setupProductsRecyclerView()
    }

    private fun setupProductsRecyclerView() {
        productsAdapter = ProductsAdapter(this, productsList)
        productsRecyclerView.apply {
            // Changed from GridLayoutManager to LinearLayoutManager for list view
            layoutManager = LinearLayoutManager(this@TailorDashboardActivity)
            adapter = productsAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        // Fetch products when activity comes to foreground
        fetchProducts()
    }

    private fun fetchProducts() {
        val url = "${Utility.apiUrl}/api/products/tailor"

        Log.d(TAG, "Fetching products from: $url")

        val request = object : JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                Log.d(TAG, "Received response with ${response.length()} products")

                productsList.clear()

                for (i in 0 until response.length()) {
                    try {
                        val productJson = response.getJSONObject(i)

                        // Debug: Log raw image data
                        val imageData = productJson.getString("productImage")
                        Log.d(TAG, "Product ${i+1} image data start: ${imageData.take(50)}...")

                        val product = Product(
                            id = productJson.getString("_id"),
                            name = productJson.getString("productName"),
                            price = productJson.getDouble("productPrice"),
                            color = productJson.getString("productColor"),
                            bodyType = productJson.getString("bodyType"),
                            description = productJson.getString("productDescription"),
                            imageUrl = imageData
                        )

                        Log.d(TAG, "Created Product object for: ${product.name}")
                        productsList.add(product)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing product JSON: ${e.message}")
                    }
                }

                Log.d(TAG, "Parsed ${productsList.size} products, updating adapter")

                // Update adapter
                productsAdapter.notifyDataSetChanged()

                // Show empty state if no products
                if (productsList.isEmpty()) {
                    findViewById<TextView>(R.id.productsTitle).text = "Your Products (None yet)"
                }
            },
            { error ->
                Log.e(TAG, "Error fetching products: ${error.message}")
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

    // Product data class
    data class Product(
        val id: String,
        val name: String,
        val price: Double,
        val color: String,
        val bodyType: String,
        val description: String,
        val imageUrl: String
    )

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
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}