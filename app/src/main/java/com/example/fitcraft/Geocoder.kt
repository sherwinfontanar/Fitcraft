package com.example.fitcraft

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import java.io.IOException
import java.util.*

class Geocoder(private val context: Context) {

    fun getAddressFromLatLng(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                addresses[0]?.getAddressLine(0)
            } else {
                "No address found"
            }
        } catch (e: IOException) {
            Log.e("GeocoderHelper", "Geocoding failed", e)
            "Geocoder service not available"
        } catch (e: IllegalArgumentException) {
            Log.e("GeocoderHelper", "Invalid lat/lng used", e)
            "Invalid coordinates"
        }
    }
}