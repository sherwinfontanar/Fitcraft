package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class PurchasesActivity : Activity() {

    private val TAG = "PurchasesActivity"
    private lateinit var noOrdersText: TextView
    private lateinit var purchasesContainer: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var loadingProgressBar: ProgressBar

    // Tab views
    private lateinit var tabToPay: TextView
    private lateinit var tabToShip: TextView
    private lateinit var tabToReceive: TextView
    private lateinit var tabCompleted: TextView
    private lateinit var tabReturns: TextView

    // Current active tab
    private var currentTab = "pending" // Default to "To Pay" tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchases)

        initializeViews()
        setupTabListeners()

        // Check if coming from successful order placement
        if (intent.getBooleanExtra("ORDER_SUCCESS", false)) {
            Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show()

            // If we're coming from a successful checkout, show the completed tab
            updateActiveTab(tabCompleted, "completed")
            titleText.text = "Completed Orders"
        } else {
            fetchOrders(currentTab) // Start with pending orders (To Pay)
        }
    }

    private fun initializeViews() {
        noOrdersText = findViewById(R.id.noOrdersText)
        purchasesContainer = findViewById(R.id.purchasesContainer)
        titleText = findViewById(R.id.titleText)

        // Initialize loading indicator
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        if (loadingProgressBar == null) {
            // Create a loading indicator programmatically if it's not in the layout
            loadingProgressBar = ProgressBar(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER
            loadingProgressBar.layoutParams = params

            // Get the parent view and add the progress bar
            val rootView = findViewById<View>(android.R.id.content)
            (rootView as ViewGroup).addView(loadingProgressBar)
        }

        // Initialize tab views
        tabToPay = findViewById(R.id.tabToPay)
        tabToShip = findViewById(R.id.tabToShip)
        tabToReceive = findViewById(R.id.tabToReceive)
        tabCompleted = findViewById(R.id.tabCompleted)
        tabReturns = findViewById(R.id.tabReturns)

        titleText.text = "Orders To Pay" // Default title
    }

    private fun setupTabListeners() {
        tabToPay.setOnClickListener {
            updateActiveTab(tabToPay, "pending")
            titleText.text = "Orders To Pay"
        }

        tabToShip.setOnClickListener {
            updateActiveTab(tabToShip, "to_ship")
            titleText.text = "Orders To Ship"
        }

        tabToReceive.setOnClickListener {
            updateActiveTab(tabToReceive, "to_receive")
            titleText.text = "Orders To Receive"
        }

        tabCompleted.setOnClickListener {
            updateActiveTab(tabCompleted, "completed")
            titleText.text = "Completed Orders"
        }

        tabReturns.setOnClickListener {
            updateActiveTab(tabReturns, "return")
            titleText.text = "Returns & Refunds"
        }
    }

    private fun updateActiveTab(activeTab: TextView, status: String) {
        // Reset all tabs to inactive
        listOf(tabToPay, tabToShip, tabToReceive, tabCompleted, tabReturns).forEach {
            it.setTextColor(resources.getColor(android.R.color.darker_gray))
            it.typeface = android.graphics.Typeface.DEFAULT
        }

        // Set active tab
        activeTab.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
        activeTab.typeface = android.graphics.Typeface.DEFAULT_BOLD

        // Update current tab and fetch orders
        currentTab = status
        fetchOrders(status)
    }

    private fun fetchOrders(status: String = "pending") {
        Log.d(TAG, "Fetching orders with status: $status")

        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE
        noOrdersText.visibility = View.GONE

        // Clear existing orders while loading
        purchasesContainer.removeAllViews()

        // Verify API URL is not null
        if (Utility.apiUrl.isNullOrEmpty()) {
            Log.e(TAG, "API URL is null or empty")
            Toast.makeText(this, "Configuration error: API URL is not set", Toast.LENGTH_LONG).show()
            loadingProgressBar.visibility = View.GONE
            noOrdersText.visibility = View.VISIBLE
            noOrdersText.text = "Configuration error. Please restart the app."
            return
        }

        val url = "${Utility.apiUrl}/api/orders"
        Log.d(TAG, "Request URL: $url")

        val queue = Volley.newRequestQueue(this)

        val jsonArrayRequest = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONArray> { response ->
                // Hide loading indicator
                loadingProgressBar.visibility = View.GONE

                // Clear existing orders
                purchasesContainer.removeAllViews()
                Log.d(TAG, "Received ${response.length()} orders")

                // Filter orders by payment status
                val filteredOrders = JSONArray()
                for (i in 0 until response.length()) {
                    val order = response.getJSONObject(i)
                    val paymentCompleted = order.optBoolean("paymentCompleted", false)

                    when (status) {
                        // To Pay tab: show orders where payment is not completed
                        "pending" -> {
                            if (!paymentCompleted) filteredOrders.put(order)
                        }

                        // To Ship tab: show orders where payment is completed
                        "to_ship" -> {
                            if (paymentCompleted) filteredOrders.put(order)
                        }

                        // For other tabs, use the regular status field if present
                        else -> {
                            val orderStatus = order.optString("status", "")
                            if (orderStatus.equals(status, ignoreCase = true)) {
                                filteredOrders.put(order)
                            }
                        }
                    }
                }

                Log.d(TAG, "Filtered to ${filteredOrders.length()} orders with status $status")

                if (filteredOrders.length() == 0) {
                    noOrdersText.visibility = View.VISIBLE
                    noOrdersText.text = "No orders found"
                    return@Listener
                }

                noOrdersText.visibility = View.GONE

                // Process each order
                for (i in 0 until filteredOrders.length()) {
                    val order = filteredOrders.getJSONObject(i)
                    displayOrder(order)
                }
            },
            Response.ErrorListener { error ->
                // Hide loading indicator
                loadingProgressBar.visibility = View.GONE

                val errorMessage = when {
                    error.networkResponse == null -> "Network error: Check your connection"
                    error.networkResponse.statusCode == 401 -> "Authentication error: Please log in again"
                    error.networkResponse.statusCode == 404 -> "API endpoint not found: Check server configuration"
                    else -> "Error fetching orders: ${error.message ?: "Unknown error"}"
                }

                Log.e(TAG, "Error fetching orders: $errorMessage", error)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                noOrdersText.visibility = View.VISIBLE
                noOrdersText.text = "Could not load orders. Please try again."
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()

                // Try to get token from Utility class first
                var token = Utility.token

                // If token is null, try to get from SharedPreferences
                if (token.isNullOrEmpty()) {
                    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    token = sharedPreferences.getString("token", "")

                    // Update the utility class with this token for future use
                    if (!token.isNullOrEmpty()) {
                        Utility.token = token
                    }
                }

                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "Authentication token is empty")
                } else {
                    Log.d(TAG, "Using token for authentication (length: ${token.length})")
                }

                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        queue.add(jsonArrayRequest)
    }

    @SuppressLint("MissingInflatedId")
    private fun displayOrder(order: JSONObject) {
        try {
            // Inflate order item layout
            val orderView = LayoutInflater.from(this).inflate(R.layout.order_item, purchasesContainer, false)

            // Find views in the inflated layout
            val productImageView = orderView.findViewById<ImageView>(R.id.ivOrderProductImage)
            val orderIdView = orderView.findViewById<TextView>(R.id.tvOrderId)
            val productNameView = orderView.findViewById<TextView>(R.id.tvOrderProductName)
            val variantView = orderView.findViewById<TextView>(R.id.tvOrderVariant)
            val priceView = orderView.findViewById<TextView>(R.id.tvOrderPrice)
            val quantityView = orderView.findViewById<TextView>(R.id.tvOrderQuantity)
            val totalAmountView = orderView.findViewById<TextView>(R.id.tvOrderTotalAmount)
            val buttonContainer = orderView.findViewById<LinearLayout>(R.id.orderButtonContainer)

            // Set order ID
            orderIdView.text = "Order #" + order.optString("orderId", "Unknown")

            // Handle order items
            if (order.has("items") && order.getJSONArray("items").length() > 0) {
                val firstItem = order.getJSONArray("items").getJSONObject(0)
                val itemCount = order.getJSONArray("items").length()

                // Display first item details
                productNameView.text = firstItem.optString("productName", "Unknown Product")
                variantView.text = firstItem.optString("productColor", "Standard")

                val price = firstItem.optDouble("productPrice", 0.0)
                priceView.text = "₱${String.format("%,.0f", price)}"

                val quantity = firstItem.optInt("quantity", 1)
                quantityView.text = "x$quantity"

                // Load product image
                val productImage = firstItem.optString("productImage", "")
                loadProductImage(productImageView, productImage)

                // Show additional items indicator if there are more items
                if (itemCount > 1) {
                    val itemCountIndicator = TextView(this)
                    itemCountIndicator.text = "+${itemCount - 1} more item${if (itemCount - 1 > 1) "s" else ""}"
                    itemCountIndicator.setTextColor(resources.getColor(android.R.color.darker_gray))
                    itemCountIndicator.textSize = 12f

                    val detailsContainer = orderView.findViewById<LinearLayout>(R.id.orderDetailsContainer)
                    detailsContainer.addView(itemCountIndicator)
                }
            } else {
                // Fallback for older order format
                productNameView.text = order.optString("productName", "Unknown Product")
                variantView.text = order.optString("productColor", "Standard")

                val price = order.optDouble("productPrice", 0.0)
                priceView.text = "₱${String.format("%,.0f", price)}"

                val quantity = order.optInt("quantity", 1)
                quantityView.text = "x$quantity"

                // Load product image
                val productImage = order.optString("productImage", "")
                loadProductImage(productImageView, productImage)
            }

            // Set total amount
            totalAmountView.text = "₱${String.format("%,.0f", order.optDouble("totalAmount", 0.0))}"

            // Add the order view to the container
            purchasesContainer.addView(orderView)

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying order: ${e.message}", e)
        }
    }

    private fun loadProductImage(imageView: ImageView, imageData: String) {
        if (imageData.isEmpty()) {
            Log.d(TAG, "Product image is empty, using placeholder")
            imageView.setImageResource(R.drawable.placeholder_image)
            return
        }

        Log.d(TAG, "Product image data length: ${imageData.length}")
        Log.d(TAG, "First 50 chars: ${imageData.take(50)}")

        // First check if it looks like a Base64 image
        if (isLikelyBase64(imageData)) {
            Log.d(TAG, "Treating as Base64 image")
            loadBase64Image(imageView, imageData)
            return
        }

        // If not Base64, try as URL
        try {
            Log.d(TAG, "Trying to load as URL: $imageData")

            // Make sure the URL has a protocol
            val imageUrl = if (!imageData.startsWith("http://") && !imageData.startsWith("https://")) {
                "https://$imageData"
            } else {
                imageData
            }

            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .resize(220, 220)
                .centerCrop()
                .into(imageView, object : Callback {
                    override fun onSuccess() {
                        Log.d(TAG, "Successfully loaded product image from URL")
                    }

                    override fun onError(e: Exception?) {
                        Log.e(TAG, "Failed to load image from URL: ${e?.message}")

                        // As fallback, try one more time with Base64 approach
                        loadBase64Image(imageView, imageData)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Exception while loading image as URL: ${e.message}", e)
            // Try Base64 as fallback
            loadBase64Image(imageView, imageData)
        }
    }

    private fun isLikelyBase64(imageString: String): Boolean {
        // Quick check for very short strings or empty strings
        if (imageString.length < 50) return false

        // Check for common base64 image prefixes
        val commonPrefixes = listOf(
            "data:image/", // Data URI scheme
            "/9j/",        // JPEG
            "iVBOR",       // PNG
            "R0lGOD",      // GIF
            "PHN2Zw"       // SVG
        )

        for (prefix in commonPrefixes) {
            if (imageString.contains(prefix)) {
                return true
            }
        }

        // Check for base64 character pattern (mostly A-Za-z0-9+/=)
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        val sampleSize = minOf(100, imageString.length)
        val sample = imageString.substring(0, sampleSize)

        var base64CharCount = 0
        for (c in sample) {
            if (c in base64Chars) {
                base64CharCount++
            }
        }

        // If more than 90% are base64 characters, likely base64
        return base64CharCount.toFloat() / sampleSize > 0.9f
    }

    private fun loadBase64Image(imageView: ImageView, imageData: String) {
        try {
            // Extract base64 part if data URI format
            var base64Image = imageData
            if (base64Image.contains("data:image")) {
                base64Image = base64Image.substring(base64Image.indexOf(",") + 1)
            }

            // Clean the string
            base64Image = base64Image.trim().replace("\\s".toRegex(), "")

            try {
                // Decode the base64 string to byte array
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)

                if (imageBytes.isEmpty()) {
                    Log.e(TAG, "Empty byte array after decoding base64")
                    imageView.setImageResource(R.drawable.placeholder_image)
                    return
                }

                // Use options to prevent OOM for large images
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                // Just get dimensions first
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                // Calculate sample size for downsampling large images
                options.inSampleSize = calculateInSampleSize(options, 500, 500)
                options.inJustDecodeBounds = false

                // Now decode with appropriate sampling
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Log.d(TAG, "Successfully set bitmap from base64")
                } else {
                    Log.e(TAG, "Failed to decode bitmap from base64")
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid base64 string: ${e.message}")
                imageView.setImageResource(R.drawable.placeholder_image)
            } catch (e: Exception) {
                Log.e(TAG, "Base64 decoding failed: ${e.message}")
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing base64 image: ${e.message}")
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}