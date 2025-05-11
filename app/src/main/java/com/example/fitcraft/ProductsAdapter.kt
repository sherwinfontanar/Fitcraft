package com.example.fitcraft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

class ProductsAdapter(
    private val context: Context,
    private val products: List<TailorDashboardActivity.Product>
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    private val TAG = "ProductsAdapter"
    // Maximum size for base64 strings to prevent OutOfMemoryError
    private val MAX_BASE64_SIZE = 1000000 // ~1MB

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImage)
        val productName: TextView = view.findViewById(R.id.productName)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val productColor: TextView = view.findViewById(R.id.productColor)
        val productDescription: TextView = view.findViewById(R.id.productDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // Set a placeholder while loading
        holder.productImage.setImageResource(R.drawable.placeholder_image)

        // Debug: Log the product being loaded
        Log.d(TAG, "Loading product: ${product.name}, position: $position")

        try {
            // Handle image loading
            if (product.imageUrl.isNotEmpty()) {
                when {
                    // Case 1: Clear URL format
                    product.imageUrl.startsWith("http://") || product.imageUrl.startsWith("https://") -> {
                        Log.d(TAG, "Loading URL image for: ${product.name}")
                        loadUrlImage(holder.productImage, product.imageUrl, product.name)
                    }
                    // Case 2: Data URI format
                    product.imageUrl.startsWith("data:image") -> {
                        Log.d(TAG, "Loading data URI image for: ${product.name}")
                        loadBase64Image(holder.productImage, product.imageUrl, product.name)
                    }
                    // Case 3: Try to detect format
                    else -> {
                        if (isLikelyBase64(product.imageUrl)) {
                            Log.d(TAG, "Loading detected base64 image for: ${product.name}")
                            loadBase64Image(holder.productImage, product.imageUrl, product.name)
                        } else {
                            Log.d(TAG, "Loading as URL by default for: ${product.name}")
                            loadUrlImage(holder.productImage, product.imageUrl, product.name)
                        }
                    }
                }
            } else {
                Log.e(TAG, "Empty image URL for product: ${product.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in image loading process for ${product.name}: ${e.message}")
            e.printStackTrace()
            // Ensure placeholder is shown on error
            holder.productImage.setImageResource(R.drawable.placeholder_image)
        }

        // Set text fields
        holder.productName.text = product.name

        // Format price with currency symbol
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        holder.productPrice.text = formatter.format(product.price)

        holder.productColor.text = "Color: ${product.color}"
        holder.productDescription.text = product.description
    }

    /**
     * Improved check if a string is likely a base64 encoded image
     */
    private fun isLikelyBase64(imageString: String): Boolean {
        // Quick length check to avoid excessive processing
        if (imageString.length < 50) return false
        if (imageString.length > MAX_BASE64_SIZE) {
            Log.w(TAG, "Base64 string too large (${imageString.length} chars), treating as URL")
            return false
        }

        // Check for common base64 image signatures
        val commonSignatures = listOf("/9j/", "iVBOR", "R0lGOD")
        for (signature in commonSignatures) {
            if (imageString.contains(signature)) {
                return true
            }
        }

        // Advanced base64 character ratio check
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        val sampleLength = minOf(100, imageString.length)
        val sampleStr = imageString.substring(0, sampleLength)

        var base64CharCount = 0
        for (c in sampleStr) {
            if (base64Chars.contains(c)) {
                base64CharCount++
            }
        }

        return base64CharCount.toFloat() / sampleLength > 0.95f
    }

    /**
     * Load base64 encoded image with improved error handling and memory management
     */
    private fun loadBase64Image(imageView: ImageView, imageData: String, productName: String) {
        try {
            // Extract base64 part if data URI format
            var base64Image = imageData
            if (base64Image.contains(",")) {
                base64Image = base64Image.substring(base64Image.indexOf(",") + 1)
            }

            // Clean the string
            base64Image = base64Image.trim().replace("\\s".toRegex(), "")

            // Check size to prevent OutOfMemoryError
            if (base64Image.length > MAX_BASE64_SIZE) {
                Log.w(TAG, "Base64 too large for $productName (${base64Image.length} chars), downsampling")
                // Continue with downsampling approach
            }

            Log.d(TAG, "Base64 string length for $productName: ${base64Image.length}")

            try {
                // Decode with options for larger images
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)

                if (imageBytes.isNotEmpty()) {
                    // Use BitmapFactory options to downsample if needed
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                    // Calculate appropriate sample size
                    options.inSampleSize = calculateInSampleSize(options, 500, 500)
                    options.inJustDecodeBounds = false

                    // Decode with sampling
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                    if (bitmap != null) {
                        imageView.post {
                            if (!imageView.isAttachedToWindow) {
                                Log.d(TAG, "ImageView detached, not setting bitmap for $productName")
                                bitmap.recycle()
                                return@post
                            }

                            imageView.setImageBitmap(bitmap)
                            Log.d(TAG, "Successfully set bitmap for $productName")
                        }
                    } else {
                        Log.e(TAG, "Failed to decode bitmap for $productName")
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                } else {
                    Log.e(TAG, "Empty byte array after decoding base64 for $productName")
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Base64 decoding failed for $productName: ${e.message}")
                // Try URL loading as fallback
                loadUrlImage(imageView, imageData, productName)
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Out of memory processing image for $productName: ${e.message}")
                System.gc() // Request garbage collection
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error processing image for $productName: ${e.message}")
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    /**
     * Calculate appropriate sample size for large bitmaps
     */
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

    /**
     * Load image from URL with improved error handling and URL sanitization
     */
    private fun loadUrlImage(imageView: ImageView, imageUrl: String, productName: String) {
        Log.d(TAG, "Loading URL image for product: $productName")

        try {
            // Clean and sanitize URL to avoid Picasso issues
            val sanitizedUrl = sanitizeUrl(imageUrl)

            if (sanitizedUrl.isEmpty()) {
                Log.e(TAG, "Invalid URL after sanitization for $productName")
                imageView.setImageResource(R.drawable.placeholder_image)
                return
            }

            // Use a different approach for Picasso
            Picasso.get()
                .load(sanitizedUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .resize(220, 220)  // Specify exact size
                .centerCrop()
                .into(imageView, object : Callback {
                    override fun onSuccess() {
                        Log.d(TAG, "Picasso successfully loaded image for $productName")
                    }

                    override fun onError(e: Exception?) {
                        Log.e(TAG, "Picasso failed to load image for $productName: ${e?.message}")

                        // Fallback to direct loading if Picasso fails
                        try {
                            // Try another approach with a different Picasso configuration
                            Picasso.Builder(context)
                                .loggingEnabled(true)
                                .build()
                                .load(sanitizedUrl)
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.placeholder_image)
                                .into(imageView)
                        } catch (e2: Exception) {
                            Log.e(TAG, "Fallback loading also failed for $productName: ${e2.message}")
                            imageView.setImageResource(R.drawable.placeholder_image)
                        }
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Picasso load for $productName: ${e.message}")
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    /**
     * Sanitize and validate URLs to handle common issues with Picasso
     */
    private fun sanitizeUrl(url: String): String {
        if (url.isEmpty()) return ""

        try {
            // Basic URL sanitization
            var sanitized = url.trim()

            // Handle spaces and special characters in URLs
            if (sanitized.contains(" ") || containsSpecialChars(sanitized)) {
                // Only encode the part after the last slash to preserve the domain
                val lastSlashIndex = sanitized.lastIndexOf("/")
                if (lastSlashIndex > 7) { // Ensure we're after http:// or https://
                    val prefix = sanitized.substring(0, lastSlashIndex + 1)
                    val suffix = sanitized.substring(lastSlashIndex + 1)
                    val encodedSuffix = URLEncoder.encode(suffix, "UTF-8")
                    sanitized = prefix + encodedSuffix
                } else {
                    // If no slash found after protocol, encode everything after protocol
                    if (sanitized.startsWith("http://")) {
                        sanitized = "http://" + URLEncoder.encode(sanitized.substring(7), "UTF-8")
                    } else if (sanitized.startsWith("https://")) {
                        sanitized = "https://" + URLEncoder.encode(sanitized.substring(8), "UTF-8")
                    } else {
                        sanitized = URLEncoder.encode(sanitized, "UTF-8")
                    }
                }
            }

            // Ensure http or https protocol
            if (!sanitized.startsWith("http://") && !sanitized.startsWith("https://")) {
                sanitized = "https://$sanitized" // Default to https
            }

            return sanitized
        } catch (e: Exception) {
            Log.e(TAG, "Error sanitizing URL: ${e.message}")
            return ""
        }
    }

    /**
     * Check if URL contains special characters that need encoding
     */
    private fun containsSpecialChars(url: String): Boolean {
        val specialChars = setOf('<', '>', '#', '%', '{', '}', '|', '\\', '^', '~', '[', ']', '`')
        return url.any { it in specialChars }
    }

    override fun getItemCount() = products.size
}