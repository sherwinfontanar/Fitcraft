package com.example.fitcraft

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility

class LandingActivity : Activity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private val productsList = mutableListOf<TailorDashboardActivity.Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        setupRecyclerView()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        fetchProducts()
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(this, productsList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = productsAdapter
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
            startActivity(Intent(this, CheckoutActivity::class.java))
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
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
