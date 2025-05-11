package com.example.fitcraft

object AppConfig {
    // Base URL for the API
    const val BASE_URL = "http://10.0.2.2:5000" // Use this for Android emulator
    // const val BASE_URL = "http://your-actual-server-ip:5000" // Use when testing with real device

    // API Endpoints
    const val ENDPOINT_LOGIN = "/api/login"
    const val ENDPOINT_REGISTER = "/api/register"
    const val ENDPOINT_PROFILE = "/api/profile"
    const val ENDPOINT_PRODUCTS = "/api/products"
    const val ENDPOINT_ORDERS = "/api/orders"
    const val ENDPOINT_MEASUREMENTS = "/api/measurement"
}