package com.example.fitcraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserDashboardActivity : Activity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GarmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        recyclerView = findViewById(R.id.rvGarments)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val bodyType = "slim" // Ideally fetched from logged-in user

        RetrofitInstance.api.getGarments(bodyType).enqueue(object : Callback<List<Garment>> {
            override fun onResponse(call: Call<List<Garment>>, response: Response<List<Garment>>) {
                if (response.isSuccessful) {
                    val garments = response.body()
                    if (garments != null && garments.isNotEmpty()) {
                        val groupedGarments = garments.groupBy { it.type }

                        // Initialize the adapter with the grouped garments
                        adapter = GarmentAdapter(groupedGarments)
                        recyclerView.adapter = adapter
                    } else {
                        Toast.makeText(this@UserDashboardActivity, "No garments available.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@UserDashboardActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Garment>>, t: Throwable) {
                Toast.makeText(this@UserDashboardActivity, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}