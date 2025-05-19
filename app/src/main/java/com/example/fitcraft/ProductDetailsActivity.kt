package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream

class ProductDetailsActivity : Activity() {

    private val TAG = "ProductDetailsActivity"
    private lateinit var productId: String
    private var productPrice: Double = 0.0
    private var productName: String = ""
    private var productColor: String = ""
    private var productImage: String = ""
    private var bodyType: String = ""


    // Added for similar products functionality
    private lateinit var recyclerView: RecyclerView
    private lateinit var similarProductsAdapter: ProductsAdapter
    private val similarProductsList = mutableListOf<TailorDashboardActivity.Product>()
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var similarProductsSection: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // Get product ID from intent
        productId = intent.getStringExtra("PRODUCT_ID") ?: ""

        if (productId.isEmpty()) {
            Toast.makeText(this, "Error: No product ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewSimilarProducts)
        loadingIndicator = findViewById(R.id.loadingProgress)
        similarProductsSection = findViewById(R.id.similarProductsSection)

        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
        }

        // Set up add to cart button
        val addToCartButton = findViewById<Button>(R.id.btnAddToCart)
        addToCartButton.setOnClickListener {
            addToCart()
        }

        // Set up buy now button
        val buyNowButton = findViewById<Button>(R.id.btnBuyNow)
        buyNowButton.setOnClickListener {
            buyNow()
        }

        // Setup similar products recycler view
        setupRecyclerView()

        // Fetch product details
        fetchProductDetails()
    }

    private fun setupRecyclerView() {
        similarProductsAdapter = ProductsAdapter(this, similarProductsList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = similarProductsAdapter
    }

    private fun fetchProductDetails() {
        val url = "${Utility.apiUrl}/api/products/$productId"
        Log.d(TAG, "Fetching product details from: $url")

        loadingIndicator.visibility = View.VISIBLE

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                displayProductDetails(response)

                // After displaying product details, fetch similar products
                fetchSimilarProducts(response.getString("productColor"))
            },
            { error ->
                Log.e(TAG, "Error fetching product details: ${error.message}")
                Toast.makeText(this, "Failed to load product details", Toast.LENGTH_SHORT).show()
                loadingIndicator.visibility = View.GONE
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

    private fun displayProductDetails(productData: JSONObject) {
        try {
            // Extract product details
            productName = productData.getString("productName")
            productPrice = productData.getDouble("productPrice")
            productColor = productData.getString("productColor")
            val productDescription = productData.getString("productDescription")

            // Extract bodyType from productData
            bodyType = if (productData.has("bodyType") && !productData.isNull("bodyType")) {
                productData.getString("bodyType")
            } else {
                ""
            }

            // Get product image URL
            productImage = if (productData.has("productImage") && !productData.isNull("productImage")) {
                productData.getString("productImage")
            } else {
                ""
            }

            Log.d(TAG, "Product image URL: $productImage")

            // Update UI with product details
            findViewById<TextView>(R.id.tvProductName).text = productName
            findViewById<TextView>(R.id.tvProductColor).text = productColor
            findViewById<TextView>(R.id.tvProductPrice).text = "₱${String.format("%,.0f", productPrice)}"
            findViewById<TextView>(R.id.tvProductDescription).text = productDescription

            // Update body type field
            findViewById<TextView>(R.id.tvBodyType).text = bodyType

            // Load product image using the fixed approach
            loadProductImage()

            // Make content visible once loaded
            findViewById<View>(R.id.contentContainer).visibility = View.VISIBLE
            loadingIndicator.visibility = View.GONE

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing product data: ${e.message}", e)
            Toast.makeText(this, "Error displaying product details", Toast.LENGTH_SHORT).show()
            loadingIndicator.visibility = View.GONE
        }
    }

    private fun loadProductImage() {
        val imgProduct = findViewById<ImageView>(R.id.imgProduct)
        imgProduct.setImageResource(R.drawable.placeholder_image)

        if (productImage.isEmpty()) {
            Log.d(TAG, "Product image is empty, using placeholder")
            findViewById<TextView>(R.id.txtImageCounter).text = "0/0"
            return
        }

        Log.d(TAG, "Product image data length: ${productImage.length}")
        Log.d(TAG, "First 50 chars: ${productImage.take(50)}")

        // First check if it looks like a Base64 image
        if (isLikelyBase64(productImage)) {
            Log.d(TAG, "Treating as Base64 image")
            loadBase64Image(imgProduct, productImage)
            return
        }

        // If not Base64, try as URL
        try {
            Log.d(TAG, "Trying to load as URL: $productImage")

            // Make sure the URL has a protocol
            val imageUrl = if (!productImage.startsWith("http://") && !productImage.startsWith("https://")) {
                "https://$productImage"
            } else {
                productImage
            }

            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(imgProduct, object : Callback {
                    override fun onSuccess() {
                        Log.d(TAG, "Successfully loaded product image from URL")
                        findViewById<TextView>(R.id.txtImageCounter).text = "1/1"
                    }

                    override fun onError(e: Exception?) {
                        Log.e(TAG, "Failed to load image from URL: ${e?.message}")

                        // As fallback, try one more time with Base64 approach
                        loadBase64Image(imgProduct, productImage)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Exception while loading image as URL: ${e.message}", e)
            // Try Base64 as fallback
            loadBase64Image(imgProduct, productImage)
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
                    findViewById<TextView>(R.id.txtImageCounter).text = "0/0"
                    return
                }

                // Use options to prevent OOM for large images
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                // Just get dimensions first
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                // Calculate sample size for downsampling large images
                options.inSampleSize = calculateInSampleSize(options, 1000, 1000)
                options.inJustDecodeBounds = false

                // Now decode with appropriate sampling
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    findViewById<TextView>(R.id.txtImageCounter).text = "1/1"
                    Log.d(TAG, "Successfully set bitmap from base64")
                } else {
                    Log.e(TAG, "Failed to decode bitmap from base64")
                    imageView.setImageResource(R.drawable.placeholder_image)
                    findViewById<TextView>(R.id.txtImageCounter).text = "0/0"
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid base64 string: ${e.message}")
                imageView.setImageResource(R.drawable.placeholder_image)
                findViewById<TextView>(R.id.txtImageCounter).text = "0/0"
            } catch (e: Exception) {
                Log.e(TAG, "Base64 decoding failed: ${e.message}")
                imageView.setImageResource(R.drawable.placeholder_image)
                findViewById<TextView>(R.id.txtImageCounter).text = "0/0"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing base64 image: ${e.message}")
            imageView.setImageResource(R.drawable.placeholder_image)
            findViewById<TextView>(R.id.txtImageCounter).text = "0/0"
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

    private fun fetchSimilarProducts(productColor: String) {
        val url = "${Utility.apiUrl}/api/products?color=$productColor"
        Log.d(TAG, "Fetching similar products from: $url")

        val request = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                similarProductsList.clear()

                // Process the similar products
                for (i in 0 until response.length()) {
                    val productJson = response.getJSONObject(i)

                    // Skip the current product
                    if (productJson.getString("_id") == productId) continue

                    val product = TailorDashboardActivity.Product(

                        id = productJson.getString("_id"),
                        name = productJson.getString("productName"),
                        price = productJson.getDouble("productPrice"),
                        color = productJson.getString("productColor"),
                        bodyType = if (productJson.has("bodyType") && !productJson.isNull("bodyType")) {
                            productJson.getString("bodyType")
                        } else {
                            ""
                        },
                        description = productJson.getString("productDescription"),
                        imageUrl = if (productJson.has("productImage") && !productJson.isNull("productImage")) {
                            productJson.getString("productImage")
                        } else {
                            ""
                        }
                    )
                    similarProductsList.add(product)
                }

                // Update UI
                if (similarProductsList.isNotEmpty()) {
                    similarProductsSection.visibility = View.VISIBLE
                    similarProductsAdapter.notifyDataSetChanged()
                } else {
                    similarProductsSection.visibility = View.GONE
                }
            },
            { error ->
                Log.e(TAG, "Error fetching similar products: ${error.message}")
                similarProductsSection.visibility = View.GONE
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

    private fun addToCart() {
        // Create cart item from product details
        val cartItem = JSONObject().apply {
            put("productId", productId)
            put("productName", productName)
            put("productPrice", productPrice)
            put("productColor", productColor)
            put("productImage", productImage)
            put("bodyType", bodyType) // Add bodyType to cart item
            put("quantity", 1)
        }

        // Store in shared preferences or send to server
        saveToLocalCart(cartItem)

        Toast.makeText(this, "$productName added to cart", Toast.LENGTH_SHORT).show()
    }

    private fun saveToLocalCart(cartItem: JSONObject) {
        val prefs = getSharedPreferences("CartPrefs", MODE_PRIVATE)
        val cartJson = prefs.getString("cart_items", "[]")

        try {
            val cartArray = if (cartJson.isNullOrEmpty()) {
                org.json.JSONArray()
            } else {
                org.json.JSONArray(cartJson)
            }

            // Check if product already exists in cart
            var existingItem = false
            for (i in 0 until cartArray.length()) {
                val item = cartArray.getJSONObject(i)
                if (item.getString("productId") == productId) {
                    // Update quantity
                    val newQuantity = item.getInt("quantity") + 1
                    item.put("quantity", newQuantity)
                    existingItem = true
                    break
                }
            }

            // Add new item if not existing
            if (!existingItem) {
                cartArray.put(cartItem)
            }

            // Save updated cart
            prefs.edit().putString("cart_items", cartArray.toString()).apply()

        } catch (e: Exception) {
            Log.e(TAG, "Error saving to cart: ${e.message}")
            Toast.makeText(this, "Failed to add item to cart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buyNow() {
        // Add to cart first
        addToCart()

        // Navigate to cart activity
        val intent = Intent(this, Cart::class.java)
        intent.putExtra("BUY_NOW", true)
        startActivity(intent)
    }
}