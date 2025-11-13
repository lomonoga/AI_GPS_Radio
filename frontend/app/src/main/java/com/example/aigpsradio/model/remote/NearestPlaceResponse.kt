package com.example.aigpsradio.model.remote

import com.google.gson.annotations.SerializedName

data class NearestPlaceResponse(
    @SerializedName("place_name") val placeName: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("track") val track: String
)
