package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class ProductUploadActivity : ComponentActivity() {

    // UI Components
    private lateinit var backButton: TextView
    private lateinit var productImageView: ImageView
    private lateinit var uploadImageButton: Button
    private lateinit var productNameEditText: EditText
    private lateinit var productPriceEditText: EditText
    private lateinit var productColorEditText: EditText
    private lateinit var productDescriptionEditText: EditText
    private lateinit var saveProductButton: Button

    // Variables for image handling
    private var imageUri: Uri? = null
    private var base64Image: String = ""
    private val TAG = "ProductUploadActivity"

    // Simple image picker launcher
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    productImageView.setImageBitmap(bitmap)
                    base64Image = convertBitmapToBase64(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_upload)

        // Initialize UI components
        initializeViews()

        // Set up click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        productImageView = findViewById(R.id.productImageView)
        uploadImageButton = findViewById(R.id.uploadImageButton)
        productNameEditText = findViewById(R.id.productNameEditText)
        productPriceEditText = findViewById(R.id.productPriceEditText)
        productColorEditText = findViewById(R.id.productColorEditText)
        productDescriptionEditText = findViewById(R.id.productDescriptionEditText)
        saveProductButton = findViewById(R.id.saveProductButton)
    }

    private fun setupClickListeners() {
        // Back button to return to previous screen

        // Simple image upload button
        uploadImageButton.setOnClickListener {
            openGallery()
        }

        // Save product button
        saveProductButton.setOnClickListener {
            if (validateInputs()) {
                saveProduct()
            }
        }
    }

    // Simple gallery opener
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(galleryIntent)
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress the image to reduce size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun validateInputs(): Boolean {
        // Check if image is selected
        if (base64Image.isEmpty()) {
            Toast.makeText(this, "Please select a product image", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check product name
        if (productNameEditText.text.toString().trim().isEmpty()) {
            productNameEditText.error = "Product name is required"
            return false
        }

        // Check product price
        if (productPriceEditText.text.toString().trim().isEmpty()) {
            productPriceEditText.error = "Product price is required"
            return false
        }

        // Check product color
        if (productColorEditText.text.toString().trim().isEmpty()) {
            productColorEditText.error = "Product color is required"
            return false
        }

        // Check product description
        if (productDescriptionEditText.text.toString().trim().isEmpty()) {
            productDescriptionEditText.error = "Product description is required"
            return false
        }

        return true
    }

    private fun saveProduct() {
        // Show loading indicator
        saveProductButton.isEnabled = false
        saveProductButton.text = "Saving..."

        // Prepare JSON data
        val productData = JSONObject().apply {
            put("productImage", base64Image)
            put("productName", productNameEditText.text.toString().trim())
            put("productPrice", productPriceEditText.text.toString().trim().toDouble())
            put("productColor", productColorEditText.text.toString().trim())
            put("productDescription", productDescriptionEditText.text.toString().trim())
        }

        // Get the token from Utility class
        val authToken = Utility.token

        if (authToken.isNullOrEmpty()) {
            Toast.makeText(this, "You need to login first", Toast.LENGTH_SHORT).show()
            saveProductButton.isEnabled = true
            saveProductButton.text = "Save Product"
            return
        }

        // Create request URL
        val url = "${Utility.apiUrl}/api/products"

        // Create the request
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            url,
            productData,
            { response ->
                Log.d(TAG, "Product saved successfully: $response")
                Toast.makeText(this, "Product saved successfully", Toast.LENGTH_SHORT).show()
                finish() // Return to previous screen
            },
            { error ->
                Log.e(TAG, "Error saving product: ${error.message}")
                Toast.makeText(this, "Failed to save product: ${error.message}", Toast.LENGTH_SHORT).show()
                saveProductButton.isEnabled = true
                saveProductButton.text = "Save Product"
            }
        ) {
            // Add auth token to headers
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${authToken ?: ""}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        // Add the request to the queue
        Volley.newRequestQueue(this).add(request)
    }
}