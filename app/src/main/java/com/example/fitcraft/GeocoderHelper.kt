package com.example.fitcraft.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import java.io.IOException
import java.util.*

/**
 * Enhanced Helper class for geocoding operations with Philippines-specific address handling
 */
class GeocoderHelper(private val context: Context) {

    // List of Philippine regions (for validation)
    private val philippineRegions = listOf(
        "NCR", "National Capital Region", "Metro Manila",
        "CAR", "Cordillera Administrative Region",
        "Region I", "Ilocos Region",
        "Region II", "Cagayan Valley",
        "Region III", "Central Luzon",
        "Region IV-A", "CALABARZON",
        "MIMAROPA", "Region IV-B",
        "Region V", "Bicol Region",
        "Region VI", "Western Visayas",
        "Region VII", "Central Visayas",
        "Region VIII", "Eastern Visayas",
        "Region IX", "Zamboanga Peninsula",
        "Region X", "Northern Mindanao",
        "Region XI", "Davao Region",
        "Region XII", "SOCCSKSARGEN",
        "Region XIII", "Caraga",
        "BARMM", "Bangsamoro"
    )

    // Complete map of regions to their provinces
    private val regionToProvinces = mapOf(
        // NCR is a special case - not technically containing provinces
        "National Capital Region" to listOf("Metro Manila"),
        "NCR" to listOf("Metro Manila"),
        "Metro Manila" to listOf("Metro Manila"),

        // Cordillera Administrative Region
        "Cordillera Administrative Region" to listOf("Abra", "Apayao", "Benguet", "Ifugao", "Kalinga", "Mountain Province"),
        "CAR" to listOf("Abra", "Apayao", "Benguet", "Ifugao", "Kalinga", "Mountain Province"),

        // Region I - Ilocos Region
        "Ilocos Region" to listOf("Ilocos Norte", "Ilocos Sur", "La Union", "Pangasinan"),
        "Region I" to listOf("Ilocos Norte", "Ilocos Sur", "La Union", "Pangasinan"),

        // Region II - Cagayan Valley
        "Cagayan Valley" to listOf("Batanes", "Cagayan", "Isabela", "Nueva Vizcaya", "Quirino"),
        "Region II" to listOf("Batanes", "Cagayan", "Isabela", "Nueva Vizcaya", "Quirino"),

        // Region III - Central Luzon
        "Central Luzon" to listOf("Aurora", "Bataan", "Bulacan", "Nueva Ecija", "Pampanga", "Tarlac", "Zambales"),
        "Region III" to listOf("Aurora", "Bataan", "Bulacan", "Nueva Ecija", "Pampanga", "Tarlac", "Zambales"),

        // Region IV-A - CALABARZON
        "CALABARZON" to listOf("Batangas", "Cavite", "Laguna", "Quezon", "Rizal"),
        "Region IV-A" to listOf("Batangas", "Cavite", "Laguna", "Quezon", "Rizal"),

        // Region IV-B - MIMAROPA
        "MIMAROPA" to listOf("Marinduque", "Occidental Mindoro", "Oriental Mindoro", "Palawan", "Romblon"),
        "Region IV-B" to listOf("Marinduque", "Occidental Mindoro", "Oriental Mindoro", "Palawan", "Romblon"),

        // Region V - Bicol Region
        "Bicol Region" to listOf("Albay", "Camarines Norte", "Camarines Sur", "Catanduanes", "Masbate", "Sorsogon"),
        "Region V" to listOf("Albay", "Camarines Norte", "Camarines Sur", "Catanduanes", "Masbate", "Sorsogon"),

        // Region VI - Western Visayas
        "Western Visayas" to listOf("Aklan", "Antique", "Capiz", "Guimaras", "Iloilo", "Negros Occidental"),
        "Region VI" to listOf("Aklan", "Antique", "Capiz", "Guimaras", "Iloilo", "Negros Occidental"),

        // Region VII - Central Visayas
        "Central Visayas" to listOf("Bohol", "Cebu", "Negros Oriental", "Siquijor"),
        "Region VII" to listOf("Bohol", "Cebu", "Negros Oriental", "Siquijor"),

        // Region VIII - Eastern Visayas
        "Eastern Visayas" to listOf("Biliran", "Eastern Samar", "Leyte", "Northern Samar", "Samar", "Southern Leyte"),
        "Region VIII" to listOf("Biliran", "Eastern Samar", "Leyte", "Northern Samar", "Samar", "Southern Leyte"),

        // Region IX - Zamboanga Peninsula
        "Zamboanga Peninsula" to listOf("Zamboanga del Norte", "Zamboanga del Sur", "Zamboanga Sibugay"),
        "Region IX" to listOf("Zamboanga del Norte", "Zamboanga del Sur", "Zamboanga Sibugay"),

        // Region X - Northern Mindanao
        "Northern Mindanao" to listOf("Bukidnon", "Camiguin", "Lanao del Norte", "Misamis Occidental", "Misamis Oriental"),
        "Region X" to listOf("Bukidnon", "Camiguin", "Lanao del Norte", "Misamis Occidental", "Misamis Oriental"),

        // Region XI - Davao Region
        "Davao Region" to listOf("Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental"),
        "Region XI" to listOf("Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental"),

        // Region XII - SOCCSKSARGEN
        "SOCCSKSARGEN" to listOf("Cotabato", "Sarangani", "South Cotabato", "Sultan Kudarat"),
        "Region XII" to listOf("Cotabato", "Sarangani", "South Cotabato", "Sultan Kudarat"),

        // Region XIII - Caraga
        "Caraga" to listOf("Agusan del Norte", "Agusan del Sur", "Dinagat Islands", "Surigao del Norte", "Surigao del Sur"),
        "Region XIII" to listOf("Agusan del Norte", "Agusan del Sur", "Dinagat Islands", "Surigao del Norte", "Surigao del Sur"),

        // BARMM - Bangsamoro Autonomous Region in Muslim Mindanao
        "Bangsamoro" to listOf("Basilan", "Lanao del Sur", "Maguindanao del Norte", "Maguindanao del Sur", "Sulu", "Tawi-Tawi"),
        "BARMM" to listOf("Basilan", "Lanao del Sur", "Maguindanao del Norte", "Maguindanao del Sur", "Sulu", "Tawi-Tawi")
    )

    /**
     * Get a full address string from latitude and longitude
     */
    fun getAddressStringFromLatLng(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                addresses[0].getAddressLine(0)
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

    /**
     * Get detailed address components from latitude and longitude with Philippines-specific handling
     * Returns a map with keys for province, city, barangay, sitio, street, buildingNumber
     */
    fun getDetailedAddressFromLatLng(latitude: Double, longitude: Double): Map<String, String> {
        val addressComponents = mutableMapOf<String, String>()
        val geocoder = Geocoder(context, Locale.getDefault())

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val fullAddressText = address.getAddressLine(0) ?: ""

                Log.d("GeocoderHelper", "Raw address data:")
                Log.d("GeocoderHelper", "adminArea: ${address.adminArea}")
                Log.d("GeocoderHelper", "subAdminArea: ${address.subAdminArea}")
                Log.d("GeocoderHelper", "locality: ${address.locality}")
                Log.d("GeocoderHelper", "subLocality: ${address.subLocality}")
                Log.d("GeocoderHelper", "thoroughfare: ${address.thoroughfare}")
                Log.d("GeocoderHelper", "subThoroughfare: ${address.subThoroughfare}")
                Log.d("GeocoderHelper", "premises: ${address.premises}")
                Log.d("GeocoderHelper", "fullAddress: $fullAddressText")

                // Use Philippines-specific logic to parse address components
                parsePhilippineAddress(address, addressComponents, fullAddressText)

                Log.d("GeocoderHelper", "Parsed components: $addressComponents")
            }
        } catch (e: Exception) {
            Log.e("GeocoderHelper", "Error getting detailed address", e)
        }

        return addressComponents
    }

    /**
     * Parse address with Philippines-specific logic
     */
    private fun parsePhilippineAddress(address: Address, components: MutableMap<String, String>, fullAddress: String) {
        val adminArea = address.adminArea ?: ""
        val subAdminArea = address.subAdminArea ?: ""

        // First, check if adminArea is a region and handle province accordingly
        if (isPhilippineRegion(adminArea)) {
            // If adminArea is a region, then subAdminArea is likely the province
            components["province"] = subAdminArea

            // If subAdminArea is empty or seems invalid, try to extract province from the region
            if (subAdminArea.isBlank() || !isLikelyProvince(subAdminArea)) {
                // Try to extract province from full address or use region-to-province mapping
                val extractedProvince = extractProvinceFromFullAddress(fullAddress, adminArea)
                if (extractedProvince.isNotEmpty()) {
                    components["province"] = extractedProvince
                }
            }
        } else {
            // If adminArea doesn't seem like a region, it might be the province directly
            components["province"] = adminArea
        }

        // Municipality/City handling - locality is typically the city
        components["city"] = address.locality ?: ""

        // If city is empty, try alternative sources
        if (components["city"].isNullOrBlank()) {
            // Check if subLocality contains city information
            if (!address.subLocality.isNullOrBlank() && isLikelyCity(address.subLocality)) {
                components["city"] = address.subLocality
            } else {
                // Try to extract city from full address
                val extractedCity = extractCityFromFullAddress(fullAddress)
                if (extractedCity.isNotEmpty()) {
                    components["city"] = extractedCity
                }
            }
        }

        // Barangay - typically in subLocality but could be elsewhere
        val possibleBarangay = when {
            !address.subLocality.isNullOrBlank() -> address.subLocality
            else -> extractBarangayFromFullAddress(fullAddress)
        }
        components["barangay"] = possibleBarangay

        // Sitio - typically not directly available, need to extract from address
        components["sitio"] = extractSitioFromFullAddress(fullAddress)

        // Street - thoroughfare typically maps to street name
        components["street"] = address.thoroughfare ?: ""

        // If street is empty, try to extract from full address
        if (components["street"].isNullOrBlank()) {
            components["street"] = extractStreetFromFullAddress(fullAddress)
        }

        // Building/House Number - subThoroughfare typically maps to building/house number
        components["buildingNumber"] = address.subThoroughfare ?: ""
    }

    private fun isPhilippineRegion(area: String?): Boolean {
        if (area.isNullOrBlank()) return false
        return philippineRegions.any { region ->
            area.equals(region, ignoreCase = true) || area.contains(region, ignoreCase = true)
        }
    }

    private fun isLikelyProvince(area: String?): Boolean {
        if (area.isNullOrBlank()) return false

        // Get all provinces from the regionToProvinces map
        val allProvinces = regionToProvinces.values.flatten().toSet()

        return allProvinces.any { province ->
            area.equals(province, ignoreCase = true) || area.contains(province, ignoreCase = true)
        }
    }

    private fun isLikelyCity(area: String?): Boolean {
        if (area.isNullOrBlank()) return false

        // Check if it contains words indicating it's a city
        return area.contains("City", ignoreCase = true) ||
                area.contains("Municipality", ignoreCase = true)
    }

    private fun extractProvinceFromFullAddress(fullAddress: String, region: String): String {
        // First try to find province from region mapping
        val possibleProvinces = regionToProvinces[region] ?: emptyList()

        for (province in possibleProvinces) {
            if (fullAddress.contains(province, ignoreCase = true)) {
                return province
            }
        }

        // Try common province extraction patterns
        val addressParts = fullAddress.split(",").map { it.trim() }

        // In Philippines, province often comes after the city/municipality
        for (i in 1 until addressParts.size) {
            val part = addressParts[i]
            if (isLikelyProvince(part)) {
                return part
            }
        }

        return ""
    }

    private fun extractCityFromFullAddress(fullAddress: String): String {
        val addressParts = fullAddress.split(",").map { it.trim() }

        // Look for parts that contain "City" or likely city names
        for (part in addressParts) {
            if (part.contains("City", ignoreCase = true) ||
                part.contains("Municipality", ignoreCase = true)) {
                return part
            }
        }

        return ""
    }

    private fun extractBarangayFromFullAddress(fullAddress: String): String {
        val addressParts = fullAddress.split(",").map { it.trim() }

        // Look for parts that contain "Barangay" or "Brgy"
        for (part in addressParts) {
            if (part.contains("Barangay", ignoreCase = true) ||
                part.contains("Brgy", ignoreCase = true)) {
                return part
            }
        }

        return ""
    }

    private fun extractSitioFromFullAddress(fullAddress: String): String {
        val addressParts = fullAddress.split(",").map { it.trim() }

        // Look for parts that contain "Sitio" or "Purok"
        for (part in addressParts) {
            if (part.contains("Sitio", ignoreCase = true) ||
                part.contains("Purok", ignoreCase = true)) {
                return part
            }
        }

        return ""
    }

    private fun extractStreetFromFullAddress(fullAddress: String): String {
        val addressParts = fullAddress.split(",").map { it.trim() }

        // Look for parts that contain "Street", "St.", "Road", "Rd.", "Avenue", "Ave."
        for (part in addressParts) {
            if (part.contains("Street", ignoreCase = true) ||
                part.contains(" St", ignoreCase = true) ||
                part.contains("Road", ignoreCase = true) ||
                part.contains(" Rd", ignoreCase = true) ||
                part.contains("Avenue", ignoreCase = true) ||
                part.contains(" Ave", ignoreCase = true)) {
                return part
            }
        }

        return ""
    }

    /**
     * Get coordinates from an address string
     */
    fun getLatLngFromAddress(addressString: String): Pair<Double, Double>? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocationName(addressString, 1)
            if (addresses?.isNotEmpty() == true) {
                Pair(addresses[0].latitude, addresses[0].longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GeocoderHelper", "Error geocoding address string", e)
            null
        }
    }
}