package com.example.fitcraft
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call
interface GarmentApi {
    @GET("garments/{bodyType}")
    fun getGarments(@Path("bodyType") bodyType: String): Call<List<Garment>>
}