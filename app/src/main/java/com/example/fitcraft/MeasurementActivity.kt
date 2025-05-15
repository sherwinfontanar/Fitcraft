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
    private lateinit var highHipValue: EditText
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
        highHipValue = findViewById(R.id.highHipValue)
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

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                updateBodyTypeInRealTime()
            }
        }

        bust.addTextChangedListener(watcher)
        waist.addTextChangedListener(watcher)
        hips.addTextChangedListener(watcher)
        highHipValue.addTextChangedListener(watcher) // ← assuming this is high hip

    }

    private fun updateBodyTypeInRealTime() {
        val bustVal = bust.text.toString().toFloatOrNull() ?: return
        val waistVal = waist.text.toString().toFloatOrNull() ?: return
        val hipsVal = hips.text.toString().toFloatOrNull() ?: return
        val highHipVal = highHipValue.text.toString().toFloatOrNull() ?: return // replace if needed

        val calculatedBodyType = determineBodyType(bustVal, waistVal, hipsVal, highHipVal)
        bodyType.setText(calculatedBodyType)
    }


    private fun setEditing(enabled: Boolean) {
        listOf(bust, waist, hips, shoulderWidth, highHipValue, neckToWaist, waistToHern, bodyType, skinColor).forEach {
            it.isEnabled = enabled
        }
        saveButton.isEnabled = enabled
        isEditing = enabled
    }

    private fun determineBodyType(bust: Float, waist: Float, hips: Float, highHip: Float): String {
        val bustMinusHips = bust - hips
        val hipsMinusBust = hips - bust
        val bustMinusWaist = bust - waist
        val hipsMinusWaist = hips - waist
        val highHipWaistRatio = if (waist != 0f) highHip / waist else 0f

        return when {
            (bustMinusHips <= 1 && hipsMinusBust < 3.6 && bustMinusWaist >= 9) ||
                    (hipsMinusWaist >= 10) -> "Hourglass"

            (hipsMinusBust >= 3.6 && hipsMinusBust < 10 &&
                    hipsMinusWaist >= 9 && highHipWaistRatio < 1.193) -> "Bottom Hourglass"

            (bustMinusHips > 1 && bustMinusHips < 10 && bustMinusWaist >= 9) -> "Top Hourglass"

            (hipsMinusBust > 2 && hipsMinusWaist >= 7 && highHipWaistRatio >= 1.193) -> "Spoon"

            (hipsMinusBust >= 3.6 && hipsMinusWaist < 9) -> "Triangle"

            (bustMinusHips >= 3.6 && bustMinusWaist < 9) -> "Inverted Triangle"

            (hipsMinusBust < 3.6 && bustMinusHips < 3.6 &&
                    bustMinusWaist < 9 && hipsMinusWaist < 10) -> "Rectangle"

            else -> "Undefined"
        }
    }



    private fun loadMeasurements() {
        val url = "${Utility.apiUrl}/api/measurement"

        val request = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                bust.setText(response.optString("bust", ""))
                waist.setText(response.optString("waist", ""))
                hips.setText(response.optString("hips", ""))
                shoulderWidth.setText(response.optString("shoulderWidth", ""))
                highHipValue.setText(response.optString("highHipValue", ""))
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
            put("neckToWaist", neckToWaist.text.toString().toFloatOrNull())
            put("waistToHern", waistToHern.text.toString().toFloatOrNull())
            val bustVal = bust.text.toString().toFloatOrNull() ?: 0f
            val waistVal = waist.text.toString().toFloatOrNull() ?: 0f
            val hipsVal = hips.text.toString().toFloatOrNull() ?: 0f
            val highHipVal = highHipValue.text.toString().toFloatOrNull() ?: 0f // ← Replace with actual high hip field if different
            val calculatedBodyType = determineBodyType(bustVal, waistVal, hipsVal, highHipVal)
            put("highHipValue", highHipValue.text.toString().toFloatOrNull())
            put("bodyType", calculatedBodyType)
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