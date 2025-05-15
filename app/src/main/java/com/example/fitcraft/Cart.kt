package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject

class Cart : Activity() {

    private val TAG = "CartActivity"
    private var cartItems = JSONArray()
    private var totalAmount = 0.0
    private val selectedItems = mutableSetOf<String>()
    private lateinit var tvTotal: TextView
    private lateinit var containerLayout: LinearLayout
    private lateinit var checkboxAllItems: CheckBox
    private lateinit var editButton: TextView
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Initialize views
        tvTotal = findViewById(R.id.tvTotal)
        containerLayout = findViewById(R.id.cartItemsContainer)
        checkboxAllItems = findViewById(R.id.checkboxAllItem)
        editButton = findViewById(R.id.btnEdit)

        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            startActivity(Intent(this, LandingActivity::class.java))
        }

        // Set up checkout button
        val checkoutButton = findViewById<Button>(R.id.btnCheckout)
        checkoutButton.setOnClickListener {
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            proceedToCheckout()
        }

        // Set up edit button
        editButton.setOnClickListener {
            toggleEditMode()
        }

        // Set up select all checkbox
        checkboxAllItems.setOnCheckedChangeListener { _, isChecked ->
            selectAllItems(isChecked)
        }

        // Load cart items
        loadCartItems()

        // Check if coming from "Buy Now" flow
        if (intent.getBooleanExtra("BUY_NOW", false)) {
            // In Buy Now flow, select last added item automatically
            if (cartItems.length() > 0) {
                val lastItem = cartItems.getJSONObject(cartItems.length() - 1)
                val productId = lastItem.getString("productId")
                selectedItems.add(productId)
                updateSelectionUI()
                updateTotalAmount()

                // Optionally, scroll to the item and proceed to checkout
                // We could add auto-checkout here, but better to let the user confirm
                Toast.makeText(this, "Ready to checkout!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCartItems() {
        try {
            // Clear existing container
            containerLayout.removeAllViews()

            // Get cart items from SharedPreferences
            val prefs = getSharedPreferences("CartPrefs", MODE_PRIVATE)
            val cartJson = prefs.getString("cart_items", "[]")
            cartItems = JSONArray(cartJson)

            if (cartItems.length() == 0) {
                // Show empty cart view
                showEmptyCartView()
                return
            }

            // Create view for each cart item
            for (i in 0 until cartItems.length()) {
                val item = cartItems.getJSONObject(i)
                addCartItemView(item, i)
            }

            // Update totals
            updateTotalAmount()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading cart items: ${e.message}")
            Toast.makeText(this, "Failed to load cart items", Toast.LENGTH_SHORT).show()
            showEmptyCartView()
        }
    }

    private fun addCartItemView(item: JSONObject, position: Int) {
        try {
            val productId = item.getString("productId")
            val productName = item.getString("productName")
            val productPrice = item.getDouble("productPrice")
            val productColor = item.getString("productColor")
            val productImage = item.getString("productImage")
            val quantity = item.getInt("quantity")

            // Log the image URL for debugging
            Log.d(TAG, "Cart item image URL: $productImage for product: $productName")

            // Inflate cart item view
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_cart, null) as CardView
            val itemLayout = itemView.findViewById<ConstraintLayout>(R.id.cartItemLayout)

            // Set item data
            val checkBox = itemLayout.findViewById<CheckBox>(R.id.checkboxItem)
            val imageView = itemLayout.findViewById<ImageView>(R.id.imageItem)
            val nameTextView = itemLayout.findViewById<TextView>(R.id.tvProductName)
            val variantTextView = itemLayout.findViewById<TextView>(R.id.tvVariant)
            val priceTextView = itemLayout.findViewById<TextView>(R.id.tvPrice)
            val quantityTextView = itemLayout.findViewById<TextView>(R.id.tvQuantity)
            val minusButton = itemLayout.findViewById<ImageButton>(R.id.btnMinus)
            val plusButton = itemLayout.findViewById<ImageButton>(R.id.btnPlus)

            // Configure view with data
            nameTextView.text = productName
            variantTextView.text = productColor
            priceTextView.text = "₱${String.format("%,.0f", productPrice)}"
            quantityTextView.text = quantity.toString()
            checkBox.isChecked = selectedItems.contains(productId)

            // Set placeholder image first
            imageView.setImageResource(R.drawable.placeholder_image)

            // Load product image using the same approach as ProductDetailsActivity
            if (productImage.isNotEmpty()) {
                loadProductImage(imageView, productImage)
            }

            // Set up checkbox listener
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(productId)
                } else {
                    selectedItems.remove(productId)
                    checkboxAllItems.isChecked = false
                }
                updateTotalAmount()
            }

            // Set up quantity buttons
            minusButton.setOnClickListener {
                updateItemQuantity(position, false)
            }

            plusButton.setOnClickListener {
                updateItemQuantity(position, true)
            }

            // Add the cart item view to container
            containerLayout.addView(itemView)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding cart item view: ${e.message}")
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

    private fun updateItemQuantity(position: Int, increase: Boolean) {
        try {
            val item = cartItems.getJSONObject(position)
            var quantity = item.getInt("quantity")

            if (increase) {
                quantity++
            } else if (quantity > 1) {
                quantity--
            } else {
                // Show remove confirmation
                showRemoveItemConfirmation(position)
                return
            }

            // Update quantity in JSON
            item.put("quantity", quantity)

            // Save updated cart
            saveCart()

            // Update UI - Reload entire cart for simplicity
            // In a production app, we would just update the specific item view
            loadCartItems()

        } catch (e: Exception) {
            Log.e(TAG, "Error updating quantity: ${e.message}")
        }
    }

    private fun showRemoveItemConfirmation(position: Int) {
        // In a real app, show a dialog to confirm removal
        // For simplicity, we'll just remove directly
        removeCartItem(position)
    }

    private fun removeCartItem(position: Int) {
        try {
            val newCartItems = JSONArray()

            // Copy all items except the one to remove
            for (i in 0 until cartItems.length()) {
                if (i != position) {
                    newCartItems.put(cartItems.getJSONObject(i))
                } else {
                    // Remove from selected items if it was selected
                    val item = cartItems.getJSONObject(i)
                    selectedItems.remove(item.getString("productId"))
                }
            }

            // Update cart items
            cartItems = newCartItems

            // Save updated cart
            saveCart()

            // Reload UI
            loadCartItems()

            Toast.makeText(this, "Item removed from cart", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error removing item: ${e.message}")
        }
    }

    private fun saveCart() {
        try {
            val prefs = getSharedPreferences("CartPrefs", MODE_PRIVATE)
            prefs.edit().putString("cart_items", cartItems.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cart: ${e.message}")
        }
    }

    private fun showEmptyCartView() {
        // In a real app, show a nice empty cart illustration
        containerLayout.removeAllViews()

        val emptyView = TextView(this)
        emptyView.text = "Your cart is empty"
        emptyView.textSize = 18f
        emptyView.setPadding(20, 100, 20, 100)
        emptyView.gravity = android.view.Gravity.CENTER

        containerLayout.addView(emptyView)

        // Update total
        tvTotal.text = "₱0"

        // Disable checkout button
        findViewById<Button>(R.id.btnCheckout).isEnabled = false
    }

    private fun updateTotalAmount() {
        totalAmount = 0.0

        try {
            for (i in 0 until cartItems.length()) {
                val item = cartItems.getJSONObject(i)
                val productId = item.getString("productId")

                if (selectedItems.contains(productId)) {
                    val price = item.getDouble("productPrice")
                    val quantity = item.getInt("quantity")
                    totalAmount += price * quantity
                }
            }

            // Update total display
            tvTotal.text = "₱${String.format("%,.0f", totalAmount)}"

            // Enable/disable checkout button
            findViewById<Button>(R.id.btnCheckout).isEnabled = selectedItems.isNotEmpty()

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total: ${e.message}")
        }
    }

    private fun selectAllItems(select: Boolean) {
        try {
            selectedItems.clear()

            if (select) {
                // Add all item IDs to selected set
                for (i in 0 until cartItems.length()) {
                    val item = cartItems.getJSONObject(i)
                    selectedItems.add(item.getString("productId"))
                }
            }

            // Update UI to reflect selection
            updateSelectionUI()

            // Update total amount
            updateTotalAmount()

        } catch (e: Exception) {
            Log.e(TAG, "Error selecting items: ${e.message}")
        }
    }

    private fun updateSelectionUI() {
        // Update checkboxes for all items
        for (i in 0 until containerLayout.childCount) {
            val cardView = containerLayout.getChildAt(i) as? CardView ?: continue
            val itemLayout = cardView.findViewById<ConstraintLayout>(R.id.cartItemLayout)
            val checkBox = itemLayout.findViewById<CheckBox>(R.id.checkboxItem)

            try {
                val item = cartItems.getJSONObject(i)
                val productId = item.getString("productId")

                // Update checkbox state without triggering listener
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = selectedItems.contains(productId)
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedItems.add(productId)
                    } else {
                        selectedItems.remove(productId)
                        checkboxAllItems.isChecked = false
                    }
                    updateTotalAmount()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error updating selection UI: ${e.message}")
            }
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        // Update button text
        editButton.text = if (isEditMode) "Done" else "Edit"

        // In a real app, show remove buttons for items in edit mode
        // For simplicity, we won't implement the full edit mode UI
    }

    private fun proceedToCheckout() {
        // Create a JSON array of selected items to pass to checkout
        val selectedItemsJson = JSONArray()

        try {
            for (i in 0 until cartItems.length()) {
                val item = cartItems.getJSONObject(i)
                val productId = item.getString("productId")

                if (selectedItems.contains(productId)) {
                    selectedItemsJson.put(item)
                }
            }

            // Store selected items for checkout
            val prefs = getSharedPreferences("CheckoutPrefs", MODE_PRIVATE)
            prefs.edit().putString("checkout_items", selectedItemsJson.toString()).apply()

            // Get user address from Profile
            val userPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val userAddress = userPrefs.getString("saved_address", null)

            // Navigate to checkout or maps for tailoring services
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("ADDRESS_TYPE", "NEARBY_TAILORS")
            intent.putExtra("CHECKOUT_TOTAL", totalAmount)

            // Pass user address if available
            if (userAddress != null) {
                intent.putExtra("USER_ADDRESS", userAddress)
            }

            startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error proceeding to checkout: ${e.message}")
            Toast.makeText(this, "Checkout failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}