package com.example.fitcraft

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LandingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_landing)

        val btnsettings = findViewById<ImageView>(R.id.settings)
        btnsettings.setOnClickListener {
            Log.e("FitCraft Home", "Settings is Clicked")


            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val profile = findViewById<ImageView>(R.id.ivProfilePicture)
        profile.setOnClickListener {
            Log.e("FitCraft Home", "Settings is Clicked")


            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        val profilename = findViewById<TextView>(R.id.profilename)
        profilename.setOnClickListener {
            Log.e("FitCraft Home", "Settings is Clicked")


            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        val homebutton = findViewById<ImageView>(R.id.homebutton)
        homebutton.setOnClickListener {
            Log.e("FitCraft Home", "Home is Clicked")


            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
        }

        val logoutButton = findViewById<ImageView>(R.id.logout)
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