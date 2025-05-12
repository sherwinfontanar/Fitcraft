package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class PurchasesActivity : Activity() {

    private lateinit var noOrdersText: TextView
    private lateinit var purchasesContainer: LinearLayout
    private lateinit var titleText: TextView

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
        Log.d("PurchasesActivity", "Fetching orders with status: $status")
        val url = "http://10.0.2.2:5000/api/orders"
        val queue = Volley.newRequestQueue(this)

        val jsonArrayRequest = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONArray> { response ->
                // Clear existing orders
                purchasesContainer.removeAllViews()
                Log.d("PurchasesActivity", "Received ${response.length()} orders")

                // Filter orders by status
                val filteredOrders = JSONArray()
                for (i in 0 until response.length()) {
                    val order = response.getJSONObject(i)
                    val orderStatus = order.optString("status", "")
                    val paymentCompleted = order.optBoolean("paymentCompleted", false)

                    // Map payment status to order status if status field is empty
                    if (orderStatus.isEmpty()) {
                        if ((status == "pending" && !paymentCompleted) ||
                            (status == "completed" && paymentCompleted)) {
                            filteredOrders.put(order)
                        }
                    } else if (orderStatus.equals(status, ignoreCase = true)) {
                        filteredOrders.put(order)
                    }
                }

                Log.d("PurchasesActivity", "Filtered to ${filteredOrders.length()} orders with status $status")

                if (filteredOrders.length() == 0) {
                    noOrdersText.visibility = View.VISIBLE
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
                Log.e("PurchasesActivity", "Error fetching orders: ${error.message}")
                Toast.makeText(this, "Error fetching orders: ${error.message}", Toast.LENGTH_LONG).show()
                noOrdersText.visibility = View.VISIBLE
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val token = sharedPreferences.getString("token", "")
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        queue.add(jsonArrayRequest)
    }

    @SuppressLint("MissingInflatedId")
    private fun displayOrder(order: JSONObject) {
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
        val payNowButton = orderView.findViewById<Button>(R.id.btnPayNow)

        // Set values from the order
        try {
            // Use orderId if available, otherwise default to "Order Details"
            orderIdView.text = if (order.has("orderId")) {
                "Order #" + order.getString("orderId")
            } else {
                "Order Details"
            }
        } catch (e: Exception) {
            orderIdView.text = "Order Details"
        }

        productNameView.text = order.optString("productName", "Unknown Product")

        // Fixed: Just use "variant" as per our server model
        variantView.text = order.optString("variant", "Standard")

        // Handle price field
        val price = if (order.has("price")) {
            order.optDouble("price", 0.0)
        } else {
            val totalAmount = order.optDouble("totalAmount", 0.0)
            val quantity = order.optInt("quantity", 1)
            if (quantity > 0) totalAmount / quantity else 0.0
        }

        priceView.text = "₱$price"
        quantityView.text = "x${order.optInt("quantity", 1)}"
        totalAmountView.text = "₱${order.optDouble("totalAmount", 0.0)}"

        // Handle the product image - now supporting multiple formats
        handleProductImage(order, productImageView)

        // Set Pay Now button visibility based on order status
        // Show "Pay Now" button only for pending orders that need payment
        if (currentTab == "pending" && !order.optBoolean("paymentCompleted", false)) {
            payNowButton.visibility = View.VISIBLE
            payNowButton.setOnClickListener {
                // Handle payment process
                handlePayment(order)
            }
        } else {
            payNowButton.visibility = View.GONE
        }

        // Add the order view to the container
        purchasesContainer.addView(orderView)
    }

    private fun handleProductImage(order: JSONObject, imageView: ImageView) {
        try {
            // Check if we have an image object with type information
            if (order.has("productImage") && !order.isNull("productImage")) {
                val imageData = order.get("productImage")

                // If it's a string, assume it's a URL or resource name
                if (imageData is String) {
                    handleImageString(imageData, imageView)
                }
                // If it's a JSONObject, check for type
                else if (imageData is JSONObject) {
                    when (imageData.optString("type", "")) {
                        "resourceId" -> {
                            // Handle resource ID
                            val resourceId = imageData.optInt("value", 0)
                            if (resourceId != 0) {
                                imageView.setImageResource(resourceId)
                            } else {
                                // Try to get resource by name
                                val resourceName = imageData.optString("resourceName", "")
                                if (resourceName.isNotEmpty()) {
                                    val resId = resources.getIdentifier(resourceName, "drawable", packageName)
                                    if (resId != 0) {
                                        imageView.setImageResource(resId)
                                    } else {
                                        imageView.setImageResource(R.drawable.placeholder_image)
                                    }
                                } else {
                                    imageView.setImageResource(R.drawable.placeholder_image)
                                }
                            }
                        }
                        "base64" -> {
                            // Handle base64 image
                            val base64String = imageData.optString("value", "")
                            if (base64String.isNotEmpty()) {
                                try {
                                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    imageView.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    imageView.setImageResource(R.drawable.placeholder_image)
                                }
                            } else {
                                imageView.setImageResource(R.drawable.placeholder_image)
                            }
                        }
                        else -> {
                            // Unknown type, use placeholder
                            imageView.setImageResource(R.drawable.placeholder_image)
                        }
                    }
                } else {
                    // Unknown format, use placeholder
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } else if (order.has("PRODUCT_IMAGE_RES_ID")) {
                // Try to get image resource ID directly
                val resourceId = order.optInt("PRODUCT_IMAGE_RES_ID", 0)
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                } else {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } else {
                // No image data, use placeholder
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            Log.e("PurchasesActivity", "Error handling product image: ${e.message}")
            // Any exception, use placeholder
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun handleImageString(imageData: String, imageView: ImageView) {
        // Check if it's a URL (starts with http)
        if (imageData.startsWith("http")) {
            loadImageWithVolley(imageData, imageView)
        } else {
            // Might be a resource name, try to load it
            try {
                val resourceId = resources.getIdentifier(imageData, "drawable", packageName)
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                } else {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    private fun loadImageWithVolley(imageUrl: String, imageView: ImageView) {
        try {
            val queue = Volley.newRequestQueue(this)
            val imageRequest = com.android.volley.toolbox.ImageRequest(
                imageUrl,
                { bitmap -> imageView.setImageBitmap(bitmap) },
                0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.ARGB_8888,
                { error ->
                    // If Volley ImageRequest fails, try alternative method
                    loadImageAlternative(imageUrl, imageView)
                }
            )
            queue.add(imageRequest)
        } catch (e: Exception) {
            // If exception occurs, use placeholder
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun loadImageAlternative(imageUrl: String, imageView: ImageView) {
        // Alternative image loading in background thread
        thread {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val input = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(input)

                // Update UI on main thread
                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // If all else fails, set placeholder on main thread
                runOnUiThread {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            }
        }
    }

    private fun handlePayment(order: JSONObject) {
        // Navigate to CheckoutActivity with order details for payment
        val intent = Intent(this, CheckoutActivity::class.java)

        // Pass order details - adapting to match CheckoutActivity's expected parameters
        intent.putExtra("ORDER_ID", order.optString("orderId", ""))

        // Add product details that CheckoutActivity expects
        if (order.has("productName")) {
            intent.putExtra("PRODUCT_NAME", order.getString("productName"))
        }

        // Handle possible variant field name differences
        if (order.has("variant")) {
            intent.putExtra("PRODUCT_VARIANT", order.getString("variant"))
        } else if (order.has("productVariant")) {
            intent.putExtra("PRODUCT_VARIANT", order.getString("productVariant"))
        }

        intent.putExtra("PRODUCT_QUANTITY", order.optInt("quantity", 1))

        // Calculate price if needed
        val price = if (order.has("price")) {
            order.optDouble("price", 0.0)
        } else {
            val totalAmount = order.optDouble("totalAmount", 0.0)
            val quantity = order.optInt("quantity", 1)
            if (quantity > 0) totalAmount / quantity else 0.0
        }

        intent.putExtra("PRODUCT_PRICE", price)
        intent.putExtra("TOTAL_AMOUNT", order.optDouble("totalAmount", 0.0))

        // Pass product image information if available
        try {
            if (order.has("productImage") && !order.isNull("productImage")) {
                val imageData = order.get("productImage")
                if (imageData is JSONObject && imageData.optString("type", "") == "resourceId") {
                    // If we have a resource ID, pass it
                    val resourceId = imageData.optInt("value", 0)
                    if (resourceId != 0) {
                        intent.putExtra("PRODUCT_IMAGE_RES_ID", resourceId)
                    } else {
                        // Try by resource name
                        val resourceName = imageData.optString("resourceName", "")
                        if (resourceName.isNotEmpty()) {
                            val resId = resources.getIdentifier(resourceName, "drawable", packageName)
                            if (resId != 0) {
                                intent.putExtra("PRODUCT_IMAGE_RES_ID", resId)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If we can't get the image resource ID, continue without it
            Log.e("PurchasesActivity", "Error handling image for payment: ${e.message}")
        }

        startActivity(intent)
    }
}