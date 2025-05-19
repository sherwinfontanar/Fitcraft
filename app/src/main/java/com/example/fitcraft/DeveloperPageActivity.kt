package com.example.fitcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView

class DeveloperPageActivity : Activity() {
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_page)
        setupScrollView()
        setupExpandableCards()

        val backlanding = findViewById<ImageButton>(R.id.btnBack)
            backlanding.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupScrollView() {
        val originalMainContainer = findViewById<LinearLayout>(R.id.main_container)
        if (originalMainContainer != null) {
            val parent = originalMainContainer.parent as ViewGroup
            val index = parent.indexOfChild(originalMainContainer)
            parent.removeView(originalMainContainer)
            val scrollView = ScrollView(this)
            scrollView.setLayoutParams(originalMainContainer.layoutParams)
            scrollView.isVerticalScrollBarEnabled = true
            scrollView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            scrollView.setBackgroundColor(Color.parseColor("#EFE9D5"))
            originalMainContainer.setLayoutParams(
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            scrollView.addView(originalMainContainer)
            parent.addView(scrollView, index)
        }
    }

    private fun setupExpandableCards() {
        val mainContainer = findViewById<LinearLayout>(R.id.main_container)
        mainContainer?.setBackgroundColor(getColor(android.R.color.transparent))
        setupMemberCard(R.id.header_lovelie, R.id.expandable_lovelie, R.id.expand_icon_lovelie)
        setupMemberCard(R.id.header_lenrick, R.id.expandable_lenrick, R.id.expand_icon_lenrick)
        setupMemberCard(R.id.header_sherwin, R.id.expandable_sherwin, R.id.expand_icon_sherwin)
    }

    private fun setupMemberCard(headerId: Int, expandableId: Int, expandIconId: Int) {
        val header = findViewById<View>(headerId)
        val expandableContent = findViewById<View>(expandableId)
        val expandIcon = findViewById<ImageView>(expandIconId)
        if (header == null || expandableContent == null || expandIcon == null) return
        val parent = header.parent as ViewGroup
        if (parent != null) {
            parent.setBackgroundResource(R.drawable.card_bg)
            val background = parent.background as GradientDrawable
            background.setColor(Color.parseColor("#27445D"))
        }
        header.setOnClickListener { v: View? ->
            val isExpanded =
                expandableContent.visibility == View.VISIBLE
            if (isExpanded) {
                expandableContent.animate().alpha(0f).setDuration(300).withEndAction {
                    expandableContent.visibility = View.GONE
                }
                expandIcon.animate().rotation(0f).setDuration(300).start()
            } else {
                expandableContent.setAlpha(0f)
                expandableContent.visibility = View.VISIBLE
                expandableContent.animate().alpha(1f).setDuration(300).start()
                expandIcon.animate().rotation(180f).setDuration(300).start()
            }
        }
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
