package com.example.fitcraft
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: GarmentApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.4:5000/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GarmentApi::class.java)
    }
}