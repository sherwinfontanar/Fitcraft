package com.example.fitcraft

data class Garment(
    val _id: String,
    val name: String,
    val image: String,
    val type: String,
    val fitFor: List<String>,
    val bodyType: List<String>
)