package com.example.fitcraft

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import com.squareup.picasso.Picasso
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import com.stripe.android.PaymentConfiguration

class CheckoutActivity<Bundle> : ComponentActivity() {

    private val TAG = "CheckoutActivity"
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var paymentIntentClientSecret: String
    private lateinit var radioCard: RadioButton
    private lateinit var radioCOD: RadioButton
    private lateinit var layoutCardInfo: LinearLayout
    private lateinit var checkoutItemsContainer: LinearLayout
    private lateinit var tvUserAddress: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvItemCount: TextView
    private lateinit var tvSubtotal: TextView

    private var checkoutItems = JSONArray()
    private var totalAmount = 0.0
    private var itemCount = 0

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51RLNm6QRbCi2Jf7PeVsjY6Bq4XQAx0B9lF0dDgtjtA0N8zVdm6YUfnJRlUs1Sbe9HQLLOSG0XyEpYh5NanHNAGgD007NDD8tYP" // Replace with your actual Stripe publishable key
        )

        initializeViews()
        loadCheckoutItems()
        loadUserAddress()
        setupPaymentSheet()
        setupListeners()
    }

    private fun initializeViews() {
        // Initialize payment options
        radioCard = findViewById(R.id.radioCard)
        radioCOD = findViewById(R.id.radioCOD)
        layoutCardInfo = findViewById(R.id.layoutCardInfo)
        checkoutItemsContainer = findViewById(R.id.checkoutItemsContainer)
        tvUserAddress = findViewById(R.id.tvUserAddress)
        tvTotal = findViewById(R.id.tvTotalAmount)
        tvItemCount = findViewById(R.id.tvItemCount)
        tvSubtotal = findViewById(R.id.tvSubtotal)

        // Set card payment as default for orders coming from Purchases
        if (intent.getBooleanExtra("FROM_PURCHASES", false)) {
            radioCard.isChecked = true
            radioCOD.isEnabled = false // Disable COD option for payments from purchases
        }

        // Set card payment info visibility based on radio selection
        radioCard.setOnCheckedChangeListener { _, isChecked ->
            layoutCardInfo.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun loadCheckoutItems() {
        try {
            // Clear existing container
            checkoutItemsContainer.removeAllViews()

            // Get checkout items from shared preferences
            val prefs = getSharedPreferences("CheckoutPrefs", MODE_PRIVATE)
            val checkoutItemsJson = prefs.getString("checkout_items", "[]")
            checkoutItems = JSONArray(checkoutItemsJson)

            if (checkoutItems.length() == 0) {
                // Show empty message
                showEmptyCheckoutMessage()
                return
            }

            // Reset total and item count
            totalAmount = 0.0
            itemCount = 0

            // Create view for each checkout item
            for (i in 0 until checkoutItems.length()) {
                val item = checkoutItems.getJSONObject(i)
                addCheckoutItemView(item)

                // Calculate total
                val itemPrice = item.getDouble("productPrice")
                val itemQuantity = item.getInt("quantity")
                totalAmount += itemPrice * itemQuantity
                itemCount += itemQuantity
            }

            // Update UI
            tvTotal.text = "₱${String.format("%,.0f", totalAmount)}"
            tvItemCount.text = "$itemCount items"
            tvSubtotal.text = "₱${String.format("%,.0f", totalAmount)}" // Add this line to update the subtotal

        } catch (e: Exception) {
            Log.e(TAG, "Error loading checkout items: ${e.message}")
            Toast.makeText(this, "Failed to load checkout items", Toast.LENGTH_SHORT).show()
            showEmptyCheckoutMessage()
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun addCheckoutItemView(item: JSONObject) {
        try {
            val productName = item.getString("productName")
            val productPrice = item.getDouble("productPrice")
            val productColor = item.getString("productColor")
            val productImage = item.getString("productImage")
            val quantity = item.getInt("quantity")

            // Inflate checkout item view
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_checkout, null)

            // Set item data
            val imageView = itemView.findViewById<ImageView>(R.id.imageItem)
            val nameTextView = itemView.findViewById<TextView>(R.id.tvProductName)
            val variantTextView = itemView.findViewById<TextView>(R.id.tvVariant)
            val priceTextView = itemView.findViewById<TextView>(R.id.tvPrice)
            val quantityTextView = itemView.findViewById<TextView>(R.id.tvQuantity)

            // Configure view with data
            nameTextView.text = productName
            variantTextView.text = productColor
            priceTextView.text = "₱${String.format("%,.0f", productPrice)}"
            quantityTextView.text = "x$quantity"

            // Set placeholder image first
            imageView.setImageResource(R.drawable.placeholder_image)

            // Load product image
            if (productImage.isNotEmpty()) {
                loadProductImage(imageView, productImage)
            }

            // Add the checkout item view to container
            checkoutItemsContainer.addView(itemView)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding checkout item view: ${e.message}")
        }
    }

    private fun loadProductImage(imageView: ImageView, imageData: String) {
        // This is the same image loading logic as in Cart.kt
        if (imageData.isEmpty()) {
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
                .into(imageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        Log.d(TAG, "Successfully loaded product image from URL")
                    }

                    override fun onError(e: Exception?) {
                        Log.e(TAG, "Failed to load image from URL: ${e?.message}")
                        // As fallback, try with Base64 approach
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

    private fun loadUserAddress() {
        try {
            // Get user address from shared preferences
            val userPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val userAddress = userPrefs.getString("saved_address", null)

            if (userAddress != null && userAddress.isNotEmpty()) {
                tvUserAddress.text = userAddress
            } else {
                tvUserAddress.text = "No address saved. Please update your profile."
                Toast.makeText(this, "Please add your address in profile settings", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user address: ${e.message}")
            tvUserAddress.text = "Error loading address"
        }
    }

    private fun showEmptyCheckoutMessage() {
        // Show message when no items to checkout
        checkoutItemsContainer.removeAllViews()

        val emptyView = TextView(this)
        emptyView.text = "No items to checkout"
        emptyView.textSize = 18f
        emptyView.setPadding(20, 100, 20, 100)
        emptyView.gravity = android.view.Gravity.CENTER

        checkoutItemsContainer.addView(emptyView)

        // Update total and item count
        tvTotal.text = "₱0"
        tvItemCount.text = "0 items"
        tvSubtotal.text = "₱0" // Add this line to reset the subtotal text

        // Disable checkout button
        findViewById<Button>(R.id.btnPlaceOrder).isEnabled = false
    }

    private fun setupPaymentSheet() {
        paymentSheet = PaymentSheet(
            this,
            { paymentSheetResult ->
                when (paymentSheetResult) {
                    is PaymentSheetResult.Completed -> {
                        // Payment successful, save order to database
                        saveOrderToDatabase(true) // true indicates payment completed
                    }
                    is PaymentSheetResult.Canceled -> {
                        Toast.makeText(this@CheckoutActivity, "Payment Cancelled", Toast.LENGTH_SHORT).show()
                    }
                    is PaymentSheetResult.Failed -> {
                        Toast.makeText(this@CheckoutActivity, "Error: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun setupListeners() {
        val placeOrder = findViewById<Button>(R.id.btnPlaceOrder)
        val back = findViewById<ImageButton>(R.id.btnBack)

        placeOrder.setOnClickListener {
            if (radioCard.isChecked) {
                fetchPaymentIntent()
            } else if (radioCOD.isChecked) {
                // Cash on Delivery selected
                saveOrderToDatabase(false) // false indicates COD (payment not completed yet)
            } else {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            }
        }

        back.setOnClickListener {
            finish() // Just go back to previous screen
        }
    }

    private fun fetchPaymentIntent() {
        val url = "${Utility.apiUrl}/api/create-payment-intent"
        val queue = Volley.newRequestQueue(this)

        // Get the token
        val token = Utility.token

        // Create JSON body with order amount and product details
        val jsonBody = JSONObject().apply {
            put("amount", totalAmount)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                paymentIntentClientSecret = response.getString("clientSecret")
                presentPaymentSheet()
            },
            { error ->
                Toast.makeText(this@CheckoutActivity, "Payment Intent error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Payment Intent error: ${error.message}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                    Log.d(TAG, "Setting Authorization header: Bearer ${token.take(10)}...")
                } else {
                    Log.e(TAG, "No token available for request")
                }
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        queue.add(request)
    }

    private fun presentPaymentSheet() {
        val configuration = PaymentSheet.Configuration.Builder("FitCraft")
            .build()
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

    private fun saveOrderToDatabase(paymentCompleted: Boolean) {
        val url = "${Utility.apiUrl}/api/orders"
        val queue = Volley.newRequestQueue(this)

        // Get the token
        val token = Utility.token

        // Get user address
        val userPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userAddress = userPrefs.getString("saved_address", "No address provided")

        // Set up JSON request body
        val jsonBody = JSONObject().apply {
            put("totalAmount", totalAmount)
            put("itemCount", itemCount)
            put("shippingAddress", userAddress)
            put("paymentCompleted", paymentCompleted)
            put("paymentMethod", if (radioCard.isChecked) "CARD" else "COD")

            // Set the correct status based on payment method
            val status = if (radioCard.isChecked) "to_ship" else "pending"
            put("status", status)

            // Add items as JSON array
            val itemsArray = JSONArray()
            for (i in 0 until checkoutItems.length()) {
                val item = checkoutItems.getJSONObject(i)
                val itemObj = JSONObject().apply {
                    put("productId", item.optString("productId", ""))
                    put("productName", item.getString("productName"))
                    put("productColor", item.getString("productColor"))
                    put("quantity", item.getInt("quantity"))
                    put("productPrice", item.getDouble("productPrice"))
                    put("productImage", item.getString("productImage"))
                }
                itemsArray.put(itemObj)
            }
            put("items", itemsArray)
        }

        Log.d(TAG, "Order request body: $jsonBody")

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                Log.d(TAG, "Order saved successfully: $response")
                Toast.makeText(this,
                    if (paymentCompleted) "Payment Successful!" else "Order placed successfully with Cash on Delivery!",
                    Toast.LENGTH_SHORT).show()

                // Clear the checkout items since the order is placed
                clearCheckoutItems()
                clearCartItems()

                // Navigate to purchases with the correct tab indicated
                navigateToPurchases(if (radioCard.isChecked) "to_ship" else "pending")
            },
            Response.ErrorListener { error ->
                // Get more detailed error information
                val networkResponse = error.networkResponse
                val statusCode = networkResponse?.statusCode ?: 0
                val responseData = networkResponse?.let {
                    String(it.data, Charsets.UTF_8)
                } ?: "No response data"

                Log.e(TAG, "Error saving order: Status code: $statusCode, Response: $responseData")
                Toast.makeText(this, "Error saving order: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                } else {
                    Log.e(TAG, "No token available for request")
                }
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        queue.add(request)
    }

    private fun clearCartItems() {
        try {
            // Get all cart items from SharedPreferences
            val prefs = getSharedPreferences("CartPrefs", MODE_PRIVATE)
            val cartJson = prefs.getString("cart_items", "[]")
            val cartItems = JSONArray(cartJson)

            // Get IDs of items being checked out
            val checkoutItemIds = mutableSetOf<String>()
            for (i in 0 until checkoutItems.length()) {
                val item = checkoutItems.getJSONObject(i)
                checkoutItemIds.add(item.optString("productId", ""))
            }

            // Create a new cart without the checked out items
            val updatedCart = JSONArray()
            for (i in 0 until cartItems.length()) {
                val item = cartItems.getJSONObject(i)
                val productId = item.optString("productId", "")
                if (!checkoutItemIds.contains(productId)) {
                    updatedCart.put(item)
                }
            }

            // Save the updated cart
            prefs.edit().putString("cart_items", updatedCart.toString()).apply()

            Log.d(TAG, "Removed ${checkoutItemIds.size} items from cart after checkout")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cart items: ${e.message}")
        }
    }

    private fun clearCheckoutItems() {
        // Clear the checkout items after successful order placement
        val prefs = getSharedPreferences("CheckoutPrefs", MODE_PRIVATE)
        prefs.edit().remove("checkout_items").apply()
    }

    private fun navigateToPurchases(orderStatus: String) {
        // Create intent with FLAG_ACTIVITY_CLEAR_TOP to clear any existing instances
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        // Pass success flag and order status
        intent.putExtra("ORDER_SUCCESS", true)
        intent.putExtra("ORDER_STATUS", orderStatus)

        startActivity(intent)
        finish() // Important to finish this activity
    }
}