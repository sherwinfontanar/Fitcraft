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
import com.google.android.material.button.MaterialButton

class Profile : Activity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

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