package com.example.aigpsradio.model.remote

import com.google.gson.annotations.SerializedName

// Request Model (модель запроса Nearest Place)

data class LocationPayload(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
