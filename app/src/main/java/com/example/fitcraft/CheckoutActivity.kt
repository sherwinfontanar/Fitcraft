package com.example.fitcraft

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import com.stripe.android.PaymentConfiguration

class CheckoutActivity : ComponentActivity() {

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var paymentIntentClientSecret: String
    private lateinit var radioCard: RadioButton
    private lateinit var radioCOD: RadioButton
    private lateinit var layoutCardInfo: LinearLayout

    // Product details from the UI
    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productVariant: TextView
    private lateinit var productQuantity: TextView
    private lateinit var productPrice: TextView
    private lateinit var totalAmount: TextView

    // Store the resource ID passed from previous screen
    private var productImageResourceId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51RLNm6QRbCi2Jf7PeVsjY6Bq4XQAx0B9lF0dDgtjtA0N8zVdm6YUfnJRlUs1Sbe9HQLLOSG0XyEpYh5NanHNAGgD007NDD8tYP" // Replace with your actual Stripe publishable key
        )
        // Get product details from intent
        getProductDetailsFromIntent()

        initializeViews()
        setupPaymentSheet()
        setupListeners()
    }

    private fun getProductDetailsFromIntent() {
        // Get product details from the intent
        intent.extras?.let { bundle ->
            productImageResourceId = bundle.getInt("PRODUCT_IMAGE_RES_ID", 0)

            // You can get other product details here as well
            // productNameStr = bundle.getString("PRODUCT_NAME", "")
            // etc.
        }
    }

    private fun initializeViews() {
        // Initialize payment options
        radioCard = findViewById(R.id.radioCard)
        radioCOD = findViewById(R.id.radioCOD)
        layoutCardInfo = findViewById(R.id.layoutCardInfo)

        // Initialize product detail views
        productImage = findViewById(R.id.ivProduct)
        productName = findViewById(R.id.tvProductName)
        productVariant = findViewById(R.id.tvVariant)
        productQuantity = findViewById(R.id.tvQuantity)
        productPrice = findViewById(R.id.tvProductPrice)
        totalAmount = findViewById(R.id.tvTotalAmount)

        // Set the product image from resource ID if it was passed
        if (productImageResourceId != 0) {
            productImage.setImageResource(productImageResourceId)
        }

        // Set card payment info visibility based on radio selection
        radioCard.setOnCheckedChangeListener { _, isChecked ->
            layoutCardInfo.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
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
            }
        }

        back.setOnClickListener {
            val intent = Intent(this, Cart::class.java)
            startActivity(intent)
        }
    }

    private fun fetchPaymentIntent() {
        val url = "${Utility.apiUrl}/api/create-payment-intent"
        val queue = Volley.newRequestQueue(this)

        // Get the token
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val token = Utility.token

        // Create JSON body with order amount and product details
        val jsonBody = JSONObject().apply {
            // You can add the product details here if needed by your backend
            put("productName", productName.text.toString())
            put("productVariant", productVariant.text.toString())
            put("quantity", productQuantity.text.toString().replace("x", "").trim().toInt())
            put("amount", totalAmount.text.toString().replace("₱", "").trim().toFloat())
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                paymentIntentClientSecret = response.getString("clientSecret")
                presentPaymentSheet()
            },
            { error ->
                Toast.makeText(this, "Payment Intent error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("CheckoutActivity", "Payment Intent error: ${error.message}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                    Log.d("CheckoutActivity", "Setting Authorization header: Bearer ${token.take(10)}...")
                } else {
                    Log.e("CheckoutActivity", "No token available for request")
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
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val token = Utility.token

        // Debug log
        Log.d("CheckoutActivity", "Using token: ${token?.take(10)}... (length: ${token?.length})")

        // Convert image to base64 string
        val imageBase64 = if (productImageResourceId != 0) {
            // Use resource name for the server
            getResourceNameById(productImageResourceId)
        } else {
            // Fallback to converting the current image to base64
            convertImageViewToBase64(productImage)
        }

        // Get quantity as integer
        val quantityStr = productQuantity.text.toString().replace("x", "").trim()
        val quantity = try {
            quantityStr.toInt()
        } catch (e: NumberFormatException) {
            Log.e("CheckoutActivity", "Error parsing quantity: $quantityStr", e)
            1 // Default to 1 if parsing fails
        }

        // Get total amount as float
        val amountStr = totalAmount.text.toString().replace("₱", "").trim()
        val amount = try {
            amountStr.toFloat()
        } catch (e: NumberFormatException) {
            Log.e("CheckoutActivity", "Error parsing amount: $amountStr", e)
            0f // Default to 0 if parsing fails
        }

        // Set up JSON request body
        val jsonBody = JSONObject().apply {
            put("productName", productName.text.toString())
            put("variant", productVariant.text.toString())
            put("quantity", quantity)
            put("totalAmount", amount)
            put("productImage", imageBase64)
            put("paymentCompleted", paymentCompleted)
        }

        Log.d("CheckoutActivity", "Request body: $jsonBody")

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                Log.d("CheckoutActivity", "Order saved successfully: $response")
                Toast.makeText(this,
                    if (paymentCompleted) "Payment Successful!" else "Order placed successfully with Cash on Delivery!",
                    Toast.LENGTH_SHORT).show()

                navigateToPurchases()
            },
            Response.ErrorListener { error ->
                // Get more detailed error information
                val networkResponse = error.networkResponse
                val statusCode = networkResponse?.statusCode ?: 0
                val responseData = networkResponse?.let {
                    String(it.data, Charsets.UTF_8)
                } ?: "No response data"

                Log.e("CheckoutActivity", "Error saving order: Status code: $statusCode, Response: $responseData")
                Toast.makeText(this, "Error saving order: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                // Make sure the token is clean and properly formatted
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                    Log.d("CheckoutActivity", "Setting Authorization header: Bearer ${token.take(10)}...")
                } else {
                    Log.e("CheckoutActivity", "No token available for request")
                }
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        queue.add(request)
    }

    // Helper method to get resource name from its ID
    private fun getResourceNameById(resId: Int): String {
        return if (resId != 0) {
            try {
                resources.getResourceEntryName(resId)
            } catch (e: Exception) {
                "unknown_image"
            }
        } else {
            "default_product_image"
        }
    }

    // Helper method to convert ImageView content to Base64 string
    private fun convertImageViewToBase64(imageView: ImageView): String {
        val drawable = imageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        return ""
    }

    private fun navigateToPurchases() {
        Log.d("CheckoutActivity", "Navigating to PurchasesActivity")

        // Create intent with FLAG_ACTIVITY_CLEAR_TOP to clear any existing instances
        val intent = Intent(this@CheckoutActivity, PurchasesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        // Pass necessary information
        intent.putExtra("ORDER_SUCCESS", true)
        if (productImageResourceId != 0) {
            intent.putExtra("PRODUCT_IMAGE_RES_ID", productImageResourceId)
        }

        startActivity(intent)
        finish() // Important to finish this activity
    }
}