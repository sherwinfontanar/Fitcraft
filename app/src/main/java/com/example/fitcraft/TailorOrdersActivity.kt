package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject

class TailorOrdersActivity : Activity() {

    private val TAG = "TailorOrdersActivity"
    private lateinit var noOrdersText: TextView
    private lateinit var ordersContainer: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var loadingProgressBar: ProgressBar

    // Tab views
    private lateinit var tabPending: TextView
    private lateinit var tabInProgress: TextView
    private lateinit var tabCompleted: TextView

    // Current active tab
    private var currentTab = "pending" // Default to "Pending" tab

    // Tailor ID (should be passed to this activity)
    private var tailorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tailor_orders)

        // Get tailor ID from intent
        tailorId = Utility.token

        if (tailorId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No tailor ID provided", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no tailor ID
            return
        }

        initializeViews()
        setupTabListeners()
        setupBackButton()

        // Start with pending orders
        fetchTailorOrders(currentTab)
    }

    private fun initializeViews() {
        noOrdersText = findViewById(R.id.noOrdersText)
        ordersContainer = findViewById(R.id.ordersContainer)
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
        tabPending = findViewById(R.id.tabPending)
        tabInProgress = findViewById(R.id.tabInProgress)
        tabCompleted = findViewById(R.id.tabCompleted)

        titleText.text = "Pending Orders" // Default title
    }

    private fun setupTabListeners() {
        tabPending.setOnClickListener {
            updateActiveTab(tabPending, "pending")
            titleText.text = "Pending Orders"
        }

        tabInProgress.setOnClickListener {
            updateActiveTab(tabInProgress, "in_progress")
            titleText.text = "Orders In Progress"
        }

        tabCompleted.setOnClickListener {
            updateActiveTab(tabCompleted, "completed")
            titleText.text = "Completed Orders"
        }
    }

    private fun setupBackButton() {
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // Return to previous screen
        }
    }

    private fun updateActiveTab(activeTab: TextView, status: String) {
        // Reset all tabs to inactive
        listOf(tabPending, tabInProgress, tabCompleted).forEach {
            it.setTextColor(resources.getColor(R.color.three))
            it.typeface = resources.getFont(R.font.poppins_semibold)
        }

        // Set active tab
        activeTab.setTextColor(resources.getColor(R.color.one))
        activeTab.typeface = resources.getFont(R.font.poppins_semibold)

        // Update current tab and fetch orders
        currentTab = status
        fetchTailorOrders(status)
    }

    private fun fetchTailorOrders(status: String = "pending") {
        Log.d(TAG, "Fetching tailor orders with status: $status for tailor: $tailorId")

        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE
        noOrdersText.visibility = View.GONE

        // Clear existing orders while loading
        ordersContainer.removeAllViews()

        // Verify API URL is not null
        if (Utility.apiUrl.isNullOrEmpty()) {
            Log.e(TAG, "API URL is null or empty")
            Toast.makeText(this, "Configuration error: API URL is not set", Toast.LENGTH_LONG).show()
            loadingProgressBar.visibility = View.GONE
            noOrdersText.visibility = View.VISIBLE
            noOrdersText.text = "Configuration error. Please restart the app."
            return
        }

        val url = "${Utility.apiUrl}/api/orders/${tailorId}?status=${status}"
        Log.d(TAG, "Request URL: $url")

        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                loadingProgressBar.visibility = View.GONE
                ordersContainer.removeAllViews()

                val dataArray = response.optJSONArray("data") ?: JSONArray()

                if (dataArray.length() == 0) {
                    noOrdersText.visibility = View.VISIBLE
                    noOrdersText.text = "No orders found"
                    return@Listener
                }

                noOrdersText.visibility = View.GONE
                for (i in 0 until dataArray.length()) {
                    val order = dataArray.getJSONObject(i)
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

                Log.e(TAG, "Error fetching tailor orders: $errorMessage", error)
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

        queue.add(jsonObjectRequest)
    }

    @SuppressLint("MissingInflatedId")
    private fun displayOrder(order: JSONObject) {
        try {
            // Inflate order item layout (using the same layout as PurchasesActivity for consistency)
            val orderView = LayoutInflater.from(this).inflate(R.layout.tailor_order_item, ordersContainer, false)

            // Find views in the inflated layout
            val productImageView = orderView.findViewById<ImageView>(R.id.ivOrderProductImage)
            val orderIdView = orderView.findViewById<TextView>(R.id.tvOrderId)
            val productNameView = orderView.findViewById<TextView>(R.id.tvOrderProductName)
            val variantView = orderView.findViewById<TextView>(R.id.tvOrderVariant)
            val priceView = orderView.findViewById<TextView>(R.id.tvOrderPrice)
            val quantityView = orderView.findViewById<TextView>(R.id.tvOrderQuantity)
            val totalAmountView = orderView.findViewById<TextView>(R.id.tvOrderTotalAmount)
            val customerNameView = orderView.findViewById<TextView>(R.id.tvCustomerName)
            val customerAddressView = orderView.findViewById<TextView>(R.id.tvCustomerAddress)
            val buttonContainer = orderView.findViewById<LinearLayout>(R.id.orderButtonContainer)

            // Set order ID
            orderIdView.text = "Order #" + order.optString("orderId", "Unknown")

            // Set customer info
            customerNameView.text = order.optString("customerName", "Unknown Customer")
            customerAddressView.text = order.optString("shippingAddress", "No address provided")

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

            // Add action buttons based on order status
            when (currentTab) {
                "pending" -> {
                    // Add "Accept Order" button
                    addActionButton(buttonContainer, "Accept Order", R.color.one) {
                        updateOrderStatus(order.optString("orderId", ""), "in_progress")
                    }

                    // Add "Reject Order" button
                    addActionButton(buttonContainer, "Reject Order", R.color.three) {
                        showRejectDialog(order.optString("orderId", ""))
                    }
                }
                "in_progress" -> {
                    // Add "Mark as Complete" button
                    addActionButton(buttonContainer, "Complete Order", R.color.one) {
                        updateOrderStatus(order.optString("orderId", ""), "completed")
                    }
                }
                "completed" -> {
                    // No action buttons needed for completed orders
                    buttonContainer.visibility = View.GONE
                }
            }

            // Add the order view to the container
            ordersContainer.addView(orderView)

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying order: ${e.message}", e)
        }
    }

    private fun addActionButton(container: LinearLayout, text: String, colorResId: Int, clickListener: () -> Unit) {
        val button = Button(this)
        button.text = text
        button.setBackgroundColor(resources.getColor(colorResId))
        button.setTextColor(resources.getColor(android.R.color.white))

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        params.setMargins(4, 0, 4, 0)
        button.layoutParams = params

        button.setOnClickListener { clickListener.invoke() }
        container.addView(button)
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        if (orderId.isEmpty()) {
            Toast.makeText(this, "Invalid order ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE

        val url = "${Utility.apiUrl}/api/tailor-orders/update-status"
        val queue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("orderId", orderId)
            put("tailorId", tailorId)
            put("status", newStatus)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                loadingProgressBar.visibility = View.GONE

                val message = when (newStatus) {
                    "in_progress" -> "Order accepted successfully"
                    "completed" -> "Order marked as completed"
                    else -> "Order status updated"
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                // Refresh the current tab
                fetchTailorOrders(currentTab)
            },
            Response.ErrorListener { error ->
                loadingProgressBar.visibility = View.GONE

                val errorMessage = "Failed to update order: ${error.message ?: "Unknown error"}"
                Log.e(TAG, errorMessage, error)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${Utility.token}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        queue.add(request)
    }

    private fun showRejectDialog(orderId: String) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Reject Order")
            .setMessage("Are you sure you want to reject this order?")
            .setPositiveButton("Yes") { _, _ ->
                rejectOrder(orderId)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun rejectOrder(orderId: String) {
        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE

        val url = "${Utility.apiUrl}/api/tailor-orders/reject"
        val queue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("orderId", orderId)
            put("tailorId", tailorId)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { _ ->
                loadingProgressBar.visibility = View.GONE
                Toast.makeText(this, "Order rejected successfully", Toast.LENGTH_SHORT).show()

                // Refresh the current tab
                fetchTailorOrders(currentTab)
            },
            Response.ErrorListener { error ->
                loadingProgressBar.visibility = View.GONE

                val errorMessage = "Failed to reject order: ${error.message ?: "Unknown error"}"
                Log.e(TAG, errorMessage, error)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${Utility.token}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        queue.add(request)
    }

    private fun loadProductImage(imageView: ImageView, imageData: String) {
        if (imageData.isEmpty()) {
            Log.d(TAG, "Product image is empty, using placeholder")
            imageView.setImageResource(R.drawable.placeholder_image)
            return
        }

        // First check if it looks like a Base64 image
        if (isLikelyBase64(imageData)) {
            loadBase64Image(imageView, imageData)
            return
        }

        // If not Base64, try as URL
        try {
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
                        // As fallback, try with Base64 approach
                        loadBase64Image(imageView, imageData)
                    }
                })
        } catch (e: Exception) {
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
                } else {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
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