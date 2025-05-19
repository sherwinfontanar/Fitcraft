package com.example.fitcraft

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TailorProfileActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tailor_profile)

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        setupNavigation()

    }
    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.homebutton).setOnClickListener {
            startActivity(Intent(this, TailorDashboardActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.myshopbutton).setOnClickListener {
            startActivity(Intent(this, TailorProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.ordersbutton).setOnClickListener {
            startActivity(Intent(this, TailorProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.logoutbutton).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_logout_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_confirm_logout).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}