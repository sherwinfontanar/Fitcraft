package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import com.google.android.material.button.MaterialButton
import org.json.JSONException

class Profile : Activity() {

    private lateinit var tvUsername: TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvUsername = findViewById(R.id.tv_username)

        fetchUserInfoFromServer()

        val profile = findViewById<MaterialButton>(R.id.btn_edit_profile)
        profile.setOnClickListener {
            Log.e("FitCraft Home", "Edit Profile is Clicked")


            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        val cart = findViewById<LinearLayout>(R.id.cartbutton)
        cart.setOnClickListener {
            Log.e("FitCraft Cart", "Cart is Clicked")


            val intent = Intent(this, Cart::class.java)
            startActivity(intent)
        }

        val homebutton = findViewById<TextView>(R.id.home)
        homebutton.setOnClickListener {
            Log.e("FitCraft Home", "Home is Clicked")


            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
        }

        val toPay = findViewById<LinearLayout>(R.id.purchasesContainer1)
        toPay.setOnClickListener {
            val intent = Intent(this, PurchasesActivity::class.java)
            startActivity(intent)
        }

        val logoutButton = findViewById<LinearLayout>(R.id.logoutbutton)
        logoutButton?.setOnClickListener { v: View? -> showLogoutDialog() }

    }

    private fun fetchUserInfoFromServer() {
        val url = "${Utility.apiUrl}/api/profile" // Adjust this URL to match your existing API

        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    // Extract user data - adjust field names based on your actual API response
                    val firstName = response.getString("firstName")
                    val lastName = response.getString("lastName")
                    val fullName = "$firstName $lastName"

                    // Update UI with just the full name
                    tvUsername.text = fullName

                    Log.d("Profile", "Updated user info from API: $fullName")
                } catch (e: JSONException) {
                    Log.e("Profile", "Error parsing profile data: ${e.message}")
                }
            },
            { error ->
                Log.e("Profile", "Error fetching profile: ${error.message}")

            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${Utility.token}"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun showLogoutDialog() {
        val dialogView: View = LayoutInflater.from(this).inflate(R.layout.activity_logout_dialog, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
        val dialog = dialogBuilder.create()
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.show()
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnConfirmLogout = dialogView.findViewById<Button>(R.id.btn_confirm_logout)
        btnCancel.setOnClickListener { v: View? -> dialog.dismiss() }
        btnConfirmLogout.setOnClickListener { v: View? ->
            dialog.dismiss()
            val intent =
                Intent(this, LoginActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }

    }
}