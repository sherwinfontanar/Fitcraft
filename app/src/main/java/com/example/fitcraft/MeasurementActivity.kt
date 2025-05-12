package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitcraft.utils.Utility
import org.json.JSONObject

class MeasurementActivity : Activity() {
    private lateinit var bust: EditText
    private lateinit var waist: EditText
    private lateinit var hips: EditText
    private lateinit var shoulderWidth: EditText
    private lateinit var sleeveLength: EditText
    private lateinit var neckToWaist: EditText
    private lateinit var waistToHern: EditText
    private lateinit var bodyType: EditText
    private lateinit var skinColor: EditText

    private lateinit var saveButton: Button
    private lateinit var editButton: ImageButton

    private var isEditing = false
    private var measurementExists = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measurement)

        bust = findViewById(R.id.bustValue)
        waist = findViewById(R.id.waistValue)
        hips = findViewById(R.id.hipsValue)
        shoulderWidth = findViewById(R.id.shoulderValue)
        sleeveLength = findViewById(R.id.sleeveValue)
        neckToWaist = findViewById(R.id.neckToWaistValue)
        waistToHern = findViewById(R.id.waistToHemValue)
        bodyType = findViewById(R.id.bodyTypeValue)
        skinColor = findViewById(R.id.skinColorValue)
        editButton = findViewById(R.id.editButton)

        saveButton = findViewById(R.id.saveButton)

        setEditing(false)
        loadMeasurements()

        editButton.setOnClickListener {
            setEditing(true)
        }

        saveButton.setOnClickListener {
            if (isEditing) {
                saveMeasurements()
            }
        }

        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setEditing(enabled: Boolean) {
        listOf(bust, waist, hips, shoulderWidth, sleeveLength, neckToWaist, waistToHern, bodyType, skinColor).forEach {
            it.isEnabled = enabled
        }
        saveButton.isEnabled = enabled
        isEditing = enabled
    }

    private fun loadMeasurements() {
        val url = "${Utility.apiUrl}/api/measurement"

        val request = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                bust.setText(response.optString("bust", ""))
                waist.setText(response.optString("waist", ""))
                hips.setText(response.optString("hips", ""))
                shoulderWidth.setText(response.optString("shoulderWidth", ""))
                sleeveLength.setText(response.optString("sleeveLength", ""))
                neckToWaist.setText(response.optString("neckToWaist", ""))
                waistToHern.setText(response.optString("waistToHern", ""))
                bodyType.setText(response.optString("bodyType", ""))
                skinColor.setText(response.optString("skinColor", ""))
                measurementExists = true
                setEditing(false)
            },
            { error ->
                if (error.networkResponse?.statusCode == 404) {
                    measurementExists = false
                    setEditing(true)
                } else {
                    Toast.makeText(this, "Error loading measurements", Toast.LENGTH_SHORT).show()
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer ${Utility.token}",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun saveMeasurements() {
        val url = "${Utility.apiUrl}/api/measurement"

        val requestBody = JSONObject().apply {
            put("bust", bust.text.toString().toFloatOrNull())
            put("waist", waist.text.toString().toFloatOrNull())
            put("hips", hips.text.toString().toFloatOrNull())
            put("shoulderWidth", shoulderWidth.text.toString().toFloatOrNull())
            put("sleeveLength", sleeveLength.text.toString().toFloatOrNull())
            put("neckToWaist", neckToWaist.text.toString().toFloatOrNull())
            put("waistToHern", waistToHern.text.toString().toFloatOrNull())
            put("bodyType", bodyType.text.toString())
            put("skinColor", skinColor.text.toString())
        }

        val request = object : JsonObjectRequest(Method.POST, url, requestBody,
            { _ ->
                Toast.makeText(this, "Measurements saved successfully!", Toast.LENGTH_SHORT).show()
                measurementExists = true
                setEditing(false)
            },
            { _ ->
                Toast.makeText(this, "Error saving measurements", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer ${Utility.token}",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}