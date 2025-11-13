package com.example.aigpsradio.model.remote

import com.google.gson.annotations.SerializedName

// Request Model (модель запроса)

data class LocationPayload(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
