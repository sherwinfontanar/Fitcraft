package com.example.fitcraft

import android.widget.*
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import androidx.activity.ComponentActivity

class CheckoutActivity : ComponentActivity() {

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var paymentIntentClientSecret: String
    private lateinit var radioCard: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        paymentSheet = PaymentSheet(
            this,
            { paymentSheetResult ->
                when (paymentSheetResult) {
                    is PaymentSheetResult.Completed -> {
                        Toast.makeText(this@CheckoutActivity, "Payment Successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@CheckoutActivity, UserProfileActivity::class.java)
                        startActivity(intent)
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

        radioCard = findViewById(R.id.radioCard)
        val placeOrder = findViewById<Button>(R.id.btnPlaceOrder)

        placeOrder.setOnClickListener {
            if (radioCard.isChecked) {
                fetchPaymentIntent()
            } else {
                val intent = Intent(this, UserProfileActivity::class.java)
                startActivity(intent)
            }
        }

        val back = findViewById<ImageButton>(R.id.btnBack)
        back.setOnClickListener {
            val intent = Intent(this, Cart::class.java)
            startActivity(intent)
        }
    }

    private fun fetchPaymentIntent() {
        val url = "http://10.0.2.2:5000/api/create-payment-intent"// Use your backend URL
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->
                paymentIntentClientSecret = response.getString("clientSecret")
                presentPaymentSheet()
            },
            { error ->
                Toast.makeText(this, "Payment Intent error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(request)
    }

    private fun presentPaymentSheet() {
        val configuration = PaymentSheet.Configuration.Builder("FitCraft")
            .build()
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

    // onPaymentSheetResult function moved to inline implementation in PaymentSheet constructor
}